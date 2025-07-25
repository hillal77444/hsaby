const { default: makeWASocket, useMultiFileAuthState, DisconnectReason } = require('baileys');
const express = require('express');
const cors = require('cors');
const pino = require('pino');
const qrcode = require('qrcode-terminal');
const fs = require('fs');
const { Boom } = require('@hapi/boom');
const crypto = require('node:crypto');
const path = require('path');

// Make crypto available globally
global.crypto = crypto;

const app = express();
app.use(cors());
app.use(express.json());

app.use((req, res, next) => {
    res.removeHeader && res.removeHeader('X-Frame-Options');
    res.setHeader('X-Frame-Options', 'ALLOWALL');
    next();
});

// تخزين الجلسات النشطة
const activeSessions = new Map();
const lastQRCodes = {};
const sessionTimeouts = new Map();
const sessionPromises = new Map();

// قائمة انتظار الرسائل المعلقة لكل جلسة
const pendingMessages = new Map(); // sessionId => [ {number, message} ]

// تخزين عدد محاولات إعادة الاتصال لكل جلسة
const reconnectAttempts = new Map();

// متغير جديد لتخزين آخر وقت إرسال رسالة لكل جلسة
const lastMessageSentAt = new Map(); // sessionId => timestamp

// متغير جديد لتتبع الجلسات التي أغلقت بسبب عدم النشاط
const closedByInactivity = {};

// نظام انتظار (Queue) حقيقي لكل جلسة
const sessionQueues = {};
function enqueue(sessionId, task) {
    if (!sessionQueues[sessionId]) {
        sessionQueues[sessionId] = Promise.resolve();
    }
    sessionQueues[sessionId] = sessionQueues[sessionId].then(() => task()).catch(() => {});
    return sessionQueues[sessionId];
}




// دالة إنشاء جلسة واتساب
async function createWhatsAppSession(sessionId) {
    // إذا كانت الجلسة نشطة بالفعل، أرجعها فورًا
    if (activeSessions.has(sessionId)) {
        console.log(`⚠️ Session ${sessionId} already exists, returning existing session`);
        return activeSessions.get(sessionId);
    }
    // إذا كان هناك عملية إنشاء جارية لنفس الجلسة، انتظرها
    if (sessionPromises.has(sessionId)) {
        console.log(`⏳ Session ${sessionId} creation in progress, waiting...`);
        return sessionPromises.get(sessionId);
    }
    
    console.log(`🚀 Starting new session creation for ${sessionId}`);
    
    // ابدأ عملية إنشاء الجلسة
    const promise = (async () => {
        let sock = null;
        try {
            const { state, saveCreds } = await useMultiFileAuthState(`sessions/${sessionId}`);
            sock = makeWASocket({
                printQRInTerminal: true,
                auth: state,
                logger: pino({ level: 'debug' }),
                browser: ['Chrome (Linux)', '', ''],
                connectTimeoutMs: 60000,
                defaultQueryTimeoutMs: 60000,
                emitOwnEvents: false,
                markOnlineOnConnect: false,
                // إضافة خيارات للاستقرار
                retryRequestDelayMs: 2000,
                maxRetries: 3
            });

            let isReconnecting = false;

            sock.ev.on('connection.update', async (update) => {
                const { connection, lastDisconnect, qr } = update;
                if (qr) {
                    qrcode.generate(qr, { small: true });
                    lastQRCodes[sessionId] = qr;
                    console.log(`📱 QR Code generated for session ${sessionId}`);
                    console.log(`🔗 QR Code length: ${qr.length} characters`);
                }
                if (connection === 'open') {
                    console.log(`✅ Session ${sessionId} connected successfully`);
                    isReconnecting = false;
                    reconnectAttempts.set(sessionId, 0); // إعادة تعيين العداد عند النجاح
                    // عند الاتصال: أرسل كل الرسائل المعلقة لهذه الجلسة فقط
                    const queue = pendingMessages.get(sessionId) || [];
                    for (const { number, message } of queue) {
                        try {
                            const formattedNumber = number.replace(/[^0-9]/g, '');
                            const fullNumber = (formattedNumber.startsWith('967') || formattedNumber.startsWith('966'))
                                ? formattedNumber
                                : `967${formattedNumber}`;
                            console.log(`[SEND][QUEUE] Sending pending message to ${fullNumber} from session ${sessionId}: "${message}"`);
                            await sock.sendMessage(`${fullNumber}@s.whatsapp.net`, { text: message, linkPreview: true });
                            console.log(`[SEND][QUEUE] Message sent successfully to ${fullNumber}`);
                            // جدولة الإغلاق بعد إرسال كل رسالة من قائمة الانتظار
                            scheduleSessionClose(sessionId, 3);
                        } catch (e) {
                            console.error(`[SEND][QUEUE][ERROR] Failed to send pending message: ${e.message}`);
                        }
                    }
                    pendingMessages.set(sessionId, []); // امسح الرسائل بعد الإرسال
                }
                if (connection === 'close') {
                    console.log(`❌ Session ${sessionId} disconnected`);
                    
                    if (lastDisconnect) {
                        console.log(`🔍 Disconnect reason for session ${sessionId}:`, {
                            error: lastDisconnect.error?.message,
                            statusCode: lastDisconnect.error?.output?.statusCode
                        });
                    }
                    
                    const shouldReconnect = (lastDisconnect?.error instanceof Boom)?.output?.statusCode !== DisconnectReason.loggedOut;
                    
                    // تحسين التعامل مع الـ conflict - لا نمنع إعادة الاتصال تماماً
                    const isConflict = lastDisconnect?.error?.output?.statusCode === 440 || 
                                     lastDisconnect?.error?.message?.includes('conflict');
                    
                    if (isConflict) {
                        console.error(`🚫 Device conflict detected for session ${sessionId}`);
                        console.error(`💡 Please close other WhatsApp sessions and try again.`);
                        // احذف الجلسة من الذاكرة لكن اسمح بإعادة إنشائها
                        activeSessions.delete(sessionId);
                        // لا تحاول إعادة الاتصال تلقائياً، لكن اسمح للمستخدم بإنشاء جلسة جديدة
                        return;
                    }
                    
                    if (shouldReconnect && !isReconnecting) {
                        // تحقق إذا كانت الجلسة أغلقت بسبب عدم النشاط
                        if (closedByInactivity[sessionId]) {
                            console.log(`[DEBUG] لن يتم إعادة تشغيل الجلسة ${sessionId} لأنها أغلقت بسبب عدم النشاط.`);
                            delete closedByInactivity[sessionId];
                            return;
                        }
                        let attempts = reconnectAttempts.get(sessionId) || 0;
                        if (attempts >= 5) {
                            console.error(`🚫 Too many reconnect attempts for session ${sessionId}. Stopping further attempts.`);
                            reconnectAttempts.set(sessionId, 0);
                            return;
                        }
                        reconnectAttempts.set(sessionId, attempts + 1);
                        isReconnecting = true;
                        console.log(`🔄 Attempting to reconnect session ${sessionId}... (attempt ${attempts + 1})`);
                        
                        // انتظر قبل إعادة الاتصال
                        setTimeout(async () => {
                            try {
                                activeSessions.delete(sessionId);
                                await createWhatsAppSession(sessionId);
                            } catch (error) {
                                console.error(`❌ Failed to reconnect session ${sessionId}:`, error);
                                isReconnecting = false;
                            }
                        }, 5000);
                    } else if (!shouldReconnect) {
                        console.log(`🚫 Session ${sessionId} logged out, not reconnecting`);
                        activeSessions.delete(sessionId);
                    }
                }
            });

            sock.ev.on('creds.update', saveCreds);
            
            // إضافة معالج لـ stream errors
            sock.ev.on('stream:error', (error) => {
                console.error(`🌊 Stream error in session ${sessionId}:`, error);
                
                // معالجة خاصة لأخطاء الـ conflict
                if (error?.content?.[0]?.tag === 'conflict' || error?.message?.includes('conflict')) {
                    console.error(`🚫 DEVICE CONFLICT detected for session ${sessionId}!`);
                    console.error(`📱 Another device is using the same WhatsApp account.`);
                    console.error(`💡 Solution: Close other WhatsApp sessions (Web, Desktop, etc.) and try again.`);
                    
                    // احذف الجلسة من الذاكرة لكن لا تمنع إعادة الإنشاء
                    activeSessions.delete(sessionId);
                    return;
                }
                
                // معالجة أخطاء أخرى
                if (error?.attrs?.code === '515' || error?.message?.includes('restart required')) {
                    console.log(`🔄 Stream error requires restart for session ${sessionId}`);
                    setTimeout(async () => {
                        try {
                            activeSessions.delete(sessionId);
                            await createWhatsAppSession(sessionId);
                        } catch (reconnectError) {
                            console.error(`❌ Failed to reconnect after stream error:`, reconnectError);
                        }
                    }, 3000);
                }
            });

            activeSessions.set(sessionId, sock);
            return sock;
        } catch (error) {
            console.error(`❌ Error creating session ${sessionId}:`, error);
            if (sock) {
                try {
                    await sock.end();
                } catch (closeError) {
                    console.error(`❌ Error closing failed session ${sessionId}:`, closeError);
                }
            }
            throw error;
        } finally {
            // بعد انتهاء الإنشاء، احذف الوعد من الخريطة
            sessionPromises.delete(sessionId);
        }
    })();
    sessionPromises.set(sessionId, promise);
    return promise;
}

function cleanSessionTempFiles(sessionId) {
    const sessionDir = path.join(__dirname, 'sessions', sessionId);
    if (fs.existsSync(sessionDir)) {
        const files = fs.readdirSync(sessionDir);
        files.forEach(file => {
            // حذف الملفات المؤقتة فقط (مثال: app-state-sync-key-*, pre-key-*, sender-key-*)
            if (
                file.startsWith('app-state-sync-key-') ||
                file.startsWith('pre-key-') ||
                file.startsWith('sender-key-')
            ) {
                try {
                    fs.unlinkSync(path.join(sessionDir, file));
                    console.log(`🧹 Deleted temp file: ${file}`);
                } catch (e) {
                    console.log(`⚠️ Failed to delete temp file: ${file}`, e);
                }
            }
        });
    }
}

function scheduleSessionClose(sessionId, minutes = 3) {
    console.log(`[DEBUG] scheduleSessionClose: سيتم إغلاق الجلسة ${sessionId} بعد ${minutes} دقائق (الوقت الحالي: ${new Date().toISOString()})`);
    if (sessionTimeouts.has(sessionId)) {
        clearTimeout(sessionTimeouts.get(sessionId));
        console.log(`[DEBUG] scheduleSessionClose: تم إعادة ضبط المؤقت لجلسة ${sessionId}`);
    }
    const timeout = setTimeout(() => {
        console.log(`[DEBUG] Timeout fired: محاولة إغلاق الجلسة ${sessionId} الآن (الوقت الحالي: ${new Date().toISOString()})`);
        const sock = activeSessions.get(sessionId);
        if (sock) {
            try {
                closedByInactivity[sessionId] = true; // ضع العلامة أولاً قبل إنهاء الجلسة
                sock.end();
                activeSessions.delete(sessionId);
                sessionTimeouts.delete(sessionId);
                cleanSessionTempFiles(sessionId);
                console.log(`✅ Session ${sessionId} closed after ${minutes} minutes of inactivity.`);
                // محاولة تحرير الذاكرة يدويًا إذا كان GC مفعلًا
                if (global.gc) {
                    console.log('[DEBUG] Forcing GC after session close');
                    global.gc();
                } else {
                    console.log('[DEBUG] GC is NOT enabled. Run node with --expose-gc to enable manual GC.');
                }
            } catch (e) {
                console.error(`[ERROR] scheduleSessionClose: فشل إغلاق الجلسة ${sessionId}:`, e);
            }
        } else {
            console.log(`[DEBUG] Timeout fired: لم يتم العثور على جلسة ${sessionId} عند محاولة الإغلاق.`);
        }
    }, minutes * 60 * 1000);
    sessionTimeouts.set(sessionId, timeout);
    console.log(`[DEBUG] scheduleSessionClose: sessionTimeouts keys الآن: [${Array.from(sessionTimeouts.keys()).join(', ')}]`);
}

// API Endpoints
app.post('/start/:sessionId', async (req, res) => {
    try {
        const { sessionId } = req.params;
        await createWhatsAppSession(sessionId);
        res.json({ success: true, message: 'Session started' });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

// إرسال رسالة
app.post('/send/:sessionId', async (req, res) => {
    const { sessionId } = req.params;
    const { number, message } = req.body;

    enqueue(sessionId, async () => {
        try {
            let sock = activeSessions.get(sessionId);
            if (!sock) {
                // الجلسة غير متصلة: أضف الرسالة للانتظار
                if (!pendingMessages.has(sessionId)) pendingMessages.set(sessionId, []);
                pendingMessages.get(sessionId).push({ number, message });
                // شغل الجلسة إذا لم تكن تعمل
                await createWhatsAppSession(sessionId);
                return res.json({ success: true, message: "سيتم إرسال الرسالة خلال 5 دقائق إذا تم الاتصال بالجهاز" });
            }
            // تحقق من وجود الرقم على واتساب
            const formattedNumber = number.replace(/[^0-9]/g, '');
            const fullNumber = (formattedNumber.startsWith('967') || formattedNumber.startsWith('966')) ? formattedNumber : `967${formattedNumber}`;
            const waId = `${fullNumber}@s.whatsapp.net`;
            const [result] = await sock.onWhatsApp(waId);
            if (!result || !result.exists) {
                return res.status(400).json({ success: false, error: 'الرقم غير مسجل في واتساب' });
            }
            await sock.sendMessage(waId, { text: message, linkPreview: true });
            scheduleSessionClose(sessionId, 3);
            lastMessageSentAt.set(sessionId, Date.now());
            return res.json({ success: true });
        } catch (error) {
            console.error(`[SEND][ERROR] Failed to send message: ${error.message}`);
            res.status(500).json({ success: false, error: error.message });
        }
    });
});

// إرسال رسائل جماعية
app.post('/send_bulk/:sessionId', async (req, res) => {
    const { sessionId } = req.params;
    const { numbers, message, delay = 1000 } = req.body;

    enqueue(sessionId, async () => {
        try {
            const sock = activeSessions.get(sessionId);
            if (!sock) {
                return res.status(404).json({ success: false, error: 'Session not found' });
            }
            const results = [];
            for (const number of numbers) {
                try {
                    const formattedNumber = number.replace(/[^0-9]/g, '');
                    const fullNumber = (formattedNumber.startsWith('967') || formattedNumber.startsWith('966')) ? formattedNumber : `967${formattedNumber}`;
                    const waId = `${fullNumber}@s.whatsapp.net`;
                    // تحقق من وجود الرقم على واتساب
                    const [result] = await sock.onWhatsApp(waId);
                    if (!result || !result.exists) {
                        results.push({ number, success: false, error: 'الرقم غير مسجل في واتساب' });
                        continue;
                    }
                    await sock.sendMessage(waId, { text: message, linkPreview: true });
                    results.push({ number, success: true });
                    await new Promise(resolve => setTimeout(resolve, delay));
                } catch (error) {
                    results.push({ number, success: false, error: error.message });
                }
            }
            res.json({ success: true, results });
        } catch (error) {
            res.status(500).json({ success: false, error: error.message });
        }
    });
});

// الحصول على حالة الجلسة
app.get('/status/:sessionId', (req, res) => {
    const { sessionId } = req.params;
    const sock = activeSessions.get(sessionId);
    
    if (!sock) {
        return res.json({ status: 'disconnected' });
    }

    res.json({ status: 'connected' });
});

// إيقاف الجلسة
app.delete('/stop/:sessionId', (req, res) => {
    const { sessionId } = req.params;
    const sock = activeSessions.get(sessionId);
    
    if (sock) {
        sock.end();
        activeSessions.delete(sessionId);
    }
    
    res.json({ success: true });
});

// عرض رمز QR
app.get('/qr/:sessionId', async (req, res) => {
    const { sessionId } = req.params;
    const sessionDir = path.join(__dirname, 'sessions', sessionId);
    const credsFile = path.join(sessionDir, 'creds.json');

    try {
        res.send(`
            <html>
                <head>
                    <title>WhatsApp QR Code</title>
                    <style>
                        body { display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background: #f0f0f0; }
                        .container { text-align: center; background: white; padding: 20px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }
                        h2 { color: #128C7E; }
                        #qrcode { margin: 20px 0; }
                        #error { color: red; margin-top: 10px; }
                        #success { color: green; margin-top: 10px; display: none; }
                        #info { color: #128C7E; margin-bottom: 10px; }
                        .btn { 
                            background: #128C7E; 
                            color: white; 
                            border: none; 
                            padding: 10px 20px; 
                            border-radius: 5px; 
                            cursor: pointer;
                            margin: 10px;
                        }
                        .btn:hover { background: #0a6960; }
                        .hidden { display: none; }
                    </style>
                    <script src="https://cdn.jsdelivr.net/npm/qrcode@1.5.1/build/qrcode.min.js"></script>
                </head>
                <body>
                    <div class="container">
                        <h2>WhatsApp QR Code</h2>
                        <div id="info">
                            <p style="color: #128C7E; font-weight: bold;">📱 امسح الباركود بهاتفك لربط WhatsApp</p>
                        </div>
                        <button id="connectBtn" class="btn hidden" onclick="startNewSession()">ربط WhatsApp جديد</button>
                        <button id="fixBtn" class="btn" onclick="fixPairing()" style="background: #410ae4ff;"> عرض الباركود</button>
                        <div id="qrcode" class="hidden"></div>
                        <div id="success" class="hidden">
                            <h3>✅ تم الاتصال بنجاح!</h3>
                            <p>يمكنك إغلاق هذه النافذة</p>
                        </div>
                        <div id="error"></div>
                    </div>
                    <script>
                        let isConnected = false;
                        let updateInterval;

                        function showError(msg) {
                            document.getElementById('error').innerText = msg;
                        }

                        async function deleteSession(sessionId) {
                            // تخزين معرف الجلسة المراد حذفها
                            document.getElementById('sessionIdToDelete').value = sessionId;
                            // عرض النموذج للحذف
                            document.getElementById('deleteModal').style.display = 'block';
                        }

                        async function startNewSession() {
                            showError('جاري إنشاء جلسة جديدة...');
                            document.getElementById('qrcode').classList.add('hidden');
                            document.getElementById('success').classList.add('hidden');
                            
                            await deleteSession();
                            
                            // إنشاء جلسة جديدة
                            try {
                                const response = await fetch('/create_session/${sessionId}', { method: 'POST' });
                                if (!response.ok) {
                                    throw new Error('Failed to create session');
                                }
                                const data = await response.json();
                                console.log('Session created:', data);
                            } catch (e) {
                                console.error('Error creating session:', e);
                                showError('خطأ في إنشاء الجلسة: ' + e.message);
                                return;
                            }
                            
                            // إظهار منطقة الباركود
                            document.getElementById('qrcode').classList.remove('hidden');
                            document.getElementById('connectBtn').classList.add('hidden');
                            
                            // انتظر قليلاً ثم ابدأ تحديث QR
                            setTimeout(() => {
                                console.log('Starting QR update...');
                                updateQR();
                                if (!updateInterval) {
                                    updateInterval = setInterval(() => {
                                        updateQR();
                                        checkConnection();
                                    }, 5000);
                                }
                            }, 3000);
                        }

                        async function checkConnection() {
                            try {
                                const response = await fetch('/check_connection/${sessionId}');
                                const data = await response.json();
                                
                                if (data.connected && !isConnected) {
                                    isConnected = true;
                                    document.getElementById('qrcode').classList.add('hidden');
                                    document.getElementById('success').classList.remove('hidden');
                                    document.getElementById('connectBtn').classList.remove('hidden');
                                    if (updateInterval) {
                                        clearInterval(updateInterval);
                                        updateInterval = null;
                                    }
                                }
                            } catch (e) {
                                console.error('Error checking connection:', e);
                            }
                        }

                        async function updateQR() {
                            if (isConnected) return;
                            
                            try {
                                const response = await fetch('/qr_data/${sessionId}');
                                const data = await response.json();
                                
                                if (data.qr) {
                                    const qrcodeDiv = document.getElementById('qrcode');
                                    qrcodeDiv.innerHTML = '';
                                    const canvas = document.createElement('canvas');
                                    canvas.id = 'qrcode-canvas';
                                    qrcodeDiv.appendChild(canvas);
                                    
                                    QRCode.toCanvas(canvas, data.qr, {
                                        width: 300,
                                        margin: 1,
                                        color: {
                                            dark: '#128C7E',
                                            light: '#ffffff'
                                        }
                                    }, function(error) {
                                        if (error) {
                                            console.error('QR Code generation error:', error);
                                            showError('حدث خطأ أثناء رسم الكود: ' + error);
                                        } else {
                                            console.log('QR Code generated successfully');
                                            document.getElementById('error').innerText = '';
                                        }
                                    });
                                } else {
                                    console.log('No QR code available yet, waiting...');
                                    document.getElementById('error').innerText = 'في انتظار رمز QR...';
                                }
                            } catch (e) {
                                console.error('Error fetching QR data:', e);
                                showError('خطأ في جلب رمز QR: ' + e);
                            }
                        }

                        async function fixPairing() {
                            showError('جاري إنشاء جلسة جديدة...');
                            document.getElementById('qrcode').classList.add('hidden');
                            document.getElementById('success').classList.add('hidden');
                            
                            try {
                                const response = await fetch('/fix_pairing/${sessionId}', { method: 'POST' });
                                if (!response.ok) {
                                    throw new Error('Failed to fix pairing');
                                }
                                const data = await response.json();
                                showError(data.message);
                                
                                // انتظر قليلاً ثم ابدأ تحديث QR
                                setTimeout(() => {
                                    console.log('Starting QR update after fix...');
                                    document.getElementById('qrcode').classList.remove('hidden');
                                    updateQR();
                                    if (!updateInterval) {
                                        updateInterval = setInterval(() => {
                                            updateQR();
                                            checkConnection();
                                        }, 5000);
                                    }
                                }, 5000); // انتظر 5 ثوانٍ
                            } catch (e) {
                                console.error('Error fixing pairing:', e);
                                showError('خطأ في إصلاح الربط: ' + e.message);
                            }
                        }

                        // Check initial connection status
                        checkConnection();
                    </script>
                </body>
            </html>
        `);
    } catch (error) {
        res.status(500).send('Error generating QR code');
    }
});

// إضافة نقطة نهاية جديدة للحصول على بيانات رمز QR
app.get('/qr_data/:sessionId', (req, res) => {
    const { sessionId } = req.params;
    const qr = lastQRCodes[sessionId];
    console.log(`QR request for session ${sessionId}:`, qr ? 'QR available' : 'No QR');
    res.json({ qr: qr || null });
});

function getSessionSize(sessionId) {
    const sessionDir = path.join(__dirname, 'sessions', sessionId);
    let totalSize = 0;
    
    function getSize(dir) {
        try {
            if (fs.existsSync(dir)) {
                const files = fs.readdirSync(dir);
                files.forEach(file => {
                    try {
                        const filePath = path.join(dir, file);
                        const stat = fs.statSync(filePath);
                        if (stat.isDirectory()) {
                            getSize(filePath);
                        } else {
                            totalSize += stat.size;
                        }
                    } catch (fileError) {
                        console.error(`Error processing file ${file}:`, fileError);
                        // استمر في معالجة الملفات الأخرى
                    }
                });
            }
        } catch (dirError) {
            console.error(`Error reading directory ${dir}:`, dirError);
            throw dirError;
        }
    }
    
    try {
        getSize(sessionDir);
        return totalSize; // بالبايت
    } catch (error) {
        console.error(`Error calculating size for session ${sessionId}:`, error);
        return 0; // إرجاع 0 في حالة الخطأ
    }
}

app.get('/session_size/:sessionId', (req, res) => {
    const { sessionId } = req.params;
    const size = getSessionSize(sessionId);
    res.json({ sessionId, sizeBytes: size, sizeMB: (size / (1024 * 1024)).toFixed(2) });
});

// --- تم التعليق مؤقتًا لمنع إنشاء الجلسة تلقائيًا عند بدء السيرفر ---
/*
const sessionId = 'admin_main';
const sessionPath = `sessions/${sessionId}`;

// التأكد من وجود مجلد الجلسة
if (!fs.existsSync('sessions')) {
    fs.mkdirSync('sessions');
}

// التحقق من وجود بيانات الجلسة
fs.readdir(sessionPath, (err, files) => {
    if (err || !files || files.length === 0) {
        // لا توجد جلسة، أنشئ جلسة جديدة
        console.log('📱 No existing session found, creating new WhatsApp session...');
        createWhatsAppSession(sessionId).then(() => {
            console.log('✅ WhatsApp session (admin_main) created and started automatically.');
        }).catch((err) => {
            console.error('❌ Failed to start WhatsApp session automatically:', err.message);
            // حاول مرة أخرى بعد 30 ثانية
            setTimeout(() => {
                console.log('🔄 Retrying session creation...');
                createWhatsAppSession(sessionId).then(() => {
                    console.log('✅ WhatsApp session (admin_main) created on retry.');
                }).catch((retryErr) => {
                    console.error('❌ Failed to start WhatsApp session on retry:', retryErr.message);
                });
            }, 30000);
        });
    } else {
        // توجد جلسة، شغّل الجلسة فقط
        console.log('📱 Existing session found, loading WhatsApp session...');
        createWhatsAppSession(sessionId).then(() => {
            console.log('✅ WhatsApp session (admin_main) loaded and started automatically.');
        }).catch((err) => {
            console.error('❌ Failed to load WhatsApp session automatically:', err.message);
            // إذا فشل تحميل الجلسة، حاول إنشاء واحدة جديدة
            console.log('🔄 Attempting to create new session due to load failure...');
            setTimeout(() => {
                createWhatsAppSession(sessionId).then(() => {
                    console.log('✅ WhatsApp session (admin_main) created after load failure.');
                }).catch((retryErr) => {
                    console.error('❌ Failed to create new session after load failure:', retryErr.message);
                });
            }, 10000);
        });
    }
});
*/
// --- نهاية التعليق ---

// Add new endpoint to check connection status
app.get('/check_connection/:sessionId', async (req, res) => {
    const { sessionId } = req.params;
    try {
        const sock = activeSessions.get(sessionId);
        res.json({ connected: sock && sock.user ? true : false });
    } catch (error) {
        res.status(500).json({ error: 'Error checking connection status' });
    }
});

// Add new endpoint to delete session

app.post('/delete_session/:sessionId', async (req, res) => {
    const { sessionId } = req.params;
    if (!sessionId || typeof sessionId !== 'string' || sessionId.trim() === '') {
        return res.status(400).json({ error: 'Session ID is required' });
    }
    try {
        console.log(`🗑️ Deleting session ${sessionId}...`);
        
        // إيقاف الجلسة بشكل كامل
        const sock = activeSessions.get(sessionId);
        if (sock) {
            console.log(`🛑 Stopping session ${sessionId}...`);
            try {
                // إيقاف جميع الأحداث
                sock.ev.removeAllListeners();
                
                // إغلاق الجلسة
                await sock.logout();
                await sock.end();
                
                console.log(`✅ Session ${sessionId} stopped successfully`);
            } catch (error) {
                console.error(`Error stopping session ${sessionId}:`, error);
            }
            activeSessions.delete(sessionId);
        }
        
        // إيقاف أي timeout للجلسة
        if (sessionTimeouts.has(sessionId)) {
            clearTimeout(sessionTimeouts.get(sessionId));
            sessionTimeouts.delete(sessionId);
        }
        
        // احذف الباركود
        delete lastQRCodes[sessionId];
        
        // احذف مجلد الجلسة
        const sessionDir = path.join(__dirname, 'sessions', sessionId);
        if (fs.existsSync(sessionDir)) {
            fs.rmSync(sessionDir, { recursive: true, force: true });
            console.log(`🗑️ Deleted session directory for ${sessionId}`);
        }
        
        res.json({ success: true, message: 'Session deleted successfully' });
    } catch (error) {
        console.error(`Error deleting session ${sessionId}:`, error);
        res.status(500).json({ error: 'Error deleting session: ' + error.message });
    }
});

// Add new endpoint to create session
app.post('/create_session/:sessionId', async (req, res) => {
    const { sessionId } = req.params;
    try {
        console.log(`🔄 Creating new session for ${sessionId}...`);
        
        // إيقاف الجلسة القديمة بشكل كامل
        const sock = activeSessions.get(sessionId);
        if (sock) {
            console.log(`🛑 Stopping existing session for ${sessionId}...`);
            try {
                // إيقاف جميع الأحداث
                sock.ev.removeAllListeners();
                
                // إغلاق الجلسة
                await sock.logout();
                await sock.end();
                
                console.log(`✅ Existing session stopped for ${sessionId}`);
            } catch (error) {
                console.error(`Error stopping existing session ${sessionId}:`, error);
            }
            activeSessions.delete(sessionId);
        }
        
        // إيقاف أي timeout للجلسة
        if (sessionTimeouts.has(sessionId)) {
            clearTimeout(sessionTimeouts.get(sessionId));
            sessionTimeouts.delete(sessionId);
        }
        
        // احذف الباركود القديم
        delete lastQRCodes[sessionId];
        
        // انتظر قليلاً قبل إنشاء الجلسة الجديدة
        await new Promise(resolve => setTimeout(resolve, 3000));
        
        await createWhatsAppSession(sessionId);
        console.log(`✅ Session ${sessionId} created successfully`);
        res.json({ success: true, message: 'Session created successfully' });
    } catch (error) {
        console.error(`Error creating session ${sessionId}:`, error);
        res.status(500).json({ error: 'Error creating session: ' + error.message });
    }
});

// Add endpoint to fix pairing issues
app.post('/fix_pairing/:sessionId', async (req, res) => {
    const { sessionId } = req.params;
    try {
        console.log(`🔧 Attempting to fix pairing issues for session ${sessionId}`);
        
        // إيقاف الجلسة الحالية بشكل كامل
        const sock = activeSessions.get(sessionId);
        if (sock) {
            console.log(`🛑 Stopping existing session for ${sessionId}...`);
            try {
                // إيقاف جميع الأحداث
                sock.ev.removeAllListeners();
                
                // إغلاق الجلسة
                await sock.logout();
                await sock.end();
                
                console.log(`✅ Existing session stopped for ${sessionId}`);
            } catch (error) {
                console.error(`Error stopping session ${sessionId}:`, error);
            }
            activeSessions.delete(sessionId);
        }
        
        // إيقاف أي timeout للجلسة
        if (sessionTimeouts.has(sessionId)) {
            clearTimeout(sessionTimeouts.get(sessionId));
            sessionTimeouts.delete(sessionId);
        }
        
        // احذف الباركود القديم
        delete lastQRCodes[sessionId];
        
        // احذف مجلد الجلسة
        const sessionDir = path.join(__dirname, 'sessions', sessionId);
        if (fs.existsSync(sessionDir)) {
            fs.rmSync(sessionDir, { recursive: true, force: true });
            console.log(`🗑️ Deleted session directory for ${sessionId}`);
        }
        
        // انتظر قليلاً
        await new Promise(resolve => setTimeout(resolve, 5000));
        
        // أنشئ جلسة جديدة
        await createWhatsAppSession(sessionId);
        console.log(`✅ New session created after fixing pairing for ${sessionId}`);
        
        res.json({ 
            success: true, 
            message: 'تم إنشاء جلسة جديدة. يرجى مسح رمز الاستجابة السريعة QR .' 
        });
    } catch (error) {
        console.error(`Error fixing pairing for session ${sessionId}:`, error);
        res.status(500).json({ error: 'Error fixing pairing: ' + error.message });
    }
});

// Add endpoint to check for device conflicts
app.get('/check_conflict/:sessionId', async (req, res) => {
    const { sessionId } = req.params;
    try {
        const sock = activeSessions.get(sessionId);
        const hasConflict = !sock; // إذا لم توجد جلسة نشطة، قد يكون هناك conflict
        
        res.json({
            sessionId,
            hasConflict,
            message: hasConflict ? 
                'Device conflict detected. Please close other WhatsApp sessions and try again.' : 
                'No conflict detected.'
        });
    } catch (error) {
        res.status(500).json({ error: 'Error checking conflict: ' + error.message });
    }
});

// Add endpoint to get all sessions information
app.get('/sessions_info', async (req, res) => {
    try {
        const sessionsInfo = [];
        const sessionsDir = path.join(__dirname, 'sessions');
        
        // التحقق من وجود مجلد الجلسات
        if (!fs.existsSync(sessionsDir)) {
            return res.json({
                totalSessions: 0,
                activeSessions: 0,
                sessions: []
            });
        }
        
        // الحصول على جميع مجلدات الجلسات
        const sessionFolders = fs.readdirSync(sessionsDir, { withFileTypes: true })
            .filter(dirent => dirent.isDirectory())
            .map(dirent => dirent.name);
        
        for (const sessionId of sessionFolders) {
            const sessionDir = path.join(sessionsDir, sessionId);
            const sock = activeSessions.get(sessionId);
            
            // حساب حجم الجلسة
            const sessionSize = getSessionSize(sessionId);
            
            // التحقق من وجود ملفات الجلسة
            const sessionFiles = fs.readdirSync(sessionDir);
            const hasCreds = sessionFiles.includes('creds.json');
            const hasStore = sessionFiles.includes('store.json');
            
            // الحصول على آخر تعديل مع معالجة الأخطاء
            let lastModified = new Date();
            try {
                const stats = fs.statSync(sessionDir);
                lastModified = stats.mtime;
            } catch (statError) {
                console.error(`Error getting stats for session ${sessionId}:`, statError);
            }
            // تنسيق التاريخ بالإنجليزي، ميلادي، توقيت اليمن
            function formatYemenDate(date) {
                try {
                    // إذا كان السيرفر يدعم Asia/Aden
                    return date.toLocaleString('en-GB', {
                        timeZone: 'Asia/Aden',
                        year: 'numeric',
                        month: '2-digit',
                        day: '2-digit',
                        hour: '2-digit',
                        minute: '2-digit',
                        second: '2-digit',
                        hour12: false
                    }).replace(',', '');
                } catch (e) {
                    // fallback: YYYY-MM-DD HH:mm:ss +03:00
                    const pad = n => n.toString().padStart(2, '0');
                    return `${date.getFullYear()}-${pad(date.getMonth()+1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())} +03:00`;
                }
            }
            const lastModifiedStr = formatYemenDate(lastModified);
            
            // التحقق من حالة الاتصال
            const isConnected = sock && sock.user ? true : false;
            const hasQR = !!lastQRCodes[sessionId];
            
            // الحصول على معلومات إضافية
            const sessionInfo = {
                sessionId,
                isActive: !!sock,
                isConnected,
                hasQR,
                hasCreds,
                hasStore,
                sessionSizeBytes: sessionSize,
                sessionSizeMB: (sessionSize / (1024 * 1024)).toFixed(2),
                lastModified: lastModifiedStr,
                lastModifiedFormatted: lastModified.toLocaleString('ar-SA'),
                reconnectAttempts: reconnectAttempts.get(sessionId) || 0,
                hasTimeout: sessionTimeouts.has(sessionId),
                pendingMessages: (pendingMessages.get(sessionId) || []).length
            };
            
            sessionsInfo.push(sessionInfo);
        }
        
        // ترتيب الجلسات حسب آخر تعديل (الأحدث أولاً)
        sessionsInfo.sort((a, b) => new Date(b.lastModified) - new Date(a.lastModified));
        
        const totalSessions = sessionsInfo.length;
        const activeSessionsCount = sessionsInfo.filter(s => s.isActive).length;
        const connectedSessions = sessionsInfo.filter(s => s.isConnected).length;
        
        res.json({
            totalSessions,
            activeSessions: activeSessionsCount,
            connectedSessions,
            sessions: sessionsInfo,
            summary: {
                total: totalSessions,
                active: activeSessionsCount,
                connected: connectedSessions,
                disconnected: activeSessionsCount - connectedSessions,
                inactive: totalSessions - activeSessionsCount
            }
        });
        
    } catch (error) {
        console.error('Error getting sessions info:', error);
        res.status(500).json({ error: 'Error getting sessions info: ' + error.message });
    }
});

// Add endpoint to get detailed session info
app.get('/session_info/:sessionId', async (req, res) => {
    const { sessionId } = req.params;
    try {
        const sessionDir = path.join(__dirname, 'sessions', sessionId);
        const sock = activeSessions.get(sessionId);
        
        if (!fs.existsSync(sessionDir)) {
            return res.status(404).json({ error: 'Session not found' });
        }
        
        // حساب حجم الجلسة
        const sessionSize = getSessionSize(sessionId);
        
        // الحصول على ملفات الجلسة
        const sessionFiles = fs.readdirSync(sessionDir);
        const fileDetails = sessionFiles.map(file => {
            const filePath = path.join(sessionDir, file);
            const stats = fs.statSync(filePath);
            return {
                name: file,
                size: stats.size,
                sizeKB: (stats.size / 1024).toFixed(2),
                lastModified: stats.mtime.toISOString(),
                lastModifiedFormatted: stats.mtime.toLocaleString('ar-SA')
            };
        });
        
        // الحصول على آخر تعديل مع معالجة الأخطاء
        let lastModified = new Date();
        try {
            const stats = fs.statSync(sessionDir);
            lastModified = stats.mtime;
        } catch (statError) {
            console.error(`Error getting stats for session ${sessionId}:`, statError);
        }
        // تنسيق التاريخ بالإنجليزي، ميلادي، توقيت اليمن
        function formatYemenDate(date) {
            try {
                // إذا كان السيرفر يدعم Asia/Aden
                return date.toLocaleString('en-GB', {
                    timeZone: 'Asia/Aden',
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit',
                    hour12: false
                }).replace(',', '');
            } catch (e) {
                // fallback: YYYY-MM-DD HH:mm:ss +03:00
                const pad = n => n.toString().padStart(2, '0');
                return `${date.getFullYear()}-${pad(date.getMonth()+1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())} +03:00`;
            }
        }
        const lastModifiedStr = formatYemenDate(lastModified);
        
        // معلومات الجلسة
        const sessionInfo = {
            sessionId,
            isActive: !!sock,
            isConnected: sock && sock.user ? true : false,
            hasQR: !!lastQRCodes[sessionId],
            sessionSizeBytes: sessionSize,
            sessionSizeMB: (sessionSize / (1024 * 1024)).toFixed(2),
            lastModified: lastModifiedStr,
            lastModifiedFormatted: lastModified.toLocaleString('ar-SA'),
            reconnectAttempts: reconnectAttempts.get(sessionId) || 0,
            hasTimeout: sessionTimeouts.has(sessionId),
            pendingMessages: (pendingMessages.get(sessionId) || []).length,
            files: fileDetails,
            fileCount: sessionFiles.length,
            hasCreds: sessionFiles.includes('creds.json'),
            hasStore: sessionFiles.includes('store.json')
        };
        
        res.json(sessionInfo);
        
    } catch (error) {
        console.error(`Error getting session info for ${sessionId}:`, error);
        res.status(500).json({ error: 'Error getting session info: ' + error.message });
    }
});

// Add endpoint to display sessions dashboard
app.get('/sessions_dashboard', async (req, res) => {
    try {
        const sessionsInfo = [];
        const sessionsDir = path.join(__dirname, 'sessions');
        
        if (fs.existsSync(sessionsDir)) {
            const sessionFolders = fs.readdirSync(sessionsDir, { withFileTypes: true })
                .filter(dirent => dirent.isDirectory())
                .map(dirent => dirent.name);
            
            for (const sessionId of sessionFolders) {
                try {
                    const sessionDir = path.join(sessionsDir, sessionId);
                    const sock = activeSessions.get(sessionId);
                    
                    // حساب حجم الجلسة مع معالجة الأخطاء
                    let sessionSize = 0;
                    try {
                        sessionSize = getSessionSize(sessionId);
                    } catch (sizeError) {
                        console.error(`Error calculating size for session ${sessionId}:`, sizeError);
                        sessionSize = 0;
                    }
                    
                    // قراءة ملفات الجلسة مع معالجة الأخطاء
                    let sessionFiles = [];
                    try {
                        sessionFiles = fs.readdirSync(sessionDir);
                    } catch (readError) {
                        console.error(`Error reading files for session ${sessionId}:`, readError);
                        sessionFiles = [];
                    }
                    
                    // الحصول على آخر تعديل مع معالجة الأخطاء
                    let lastModified = new Date();
                    try {
                        const stats = fs.statSync(sessionDir);
                        lastModified = stats.mtime;
                    } catch (statError) {
                        console.error(`Error getting stats for session ${sessionId}:`, statError);
                    }
                    // تنسيق التاريخ بالإنجليزي، ميلادي، توقيت اليمن
                    function formatYemenDate(date) {
                        try {
                            // إذا كان السيرفر يدعم Asia/Aden
                            return date.toLocaleString('en-GB', {
                                timeZone: 'Asia/Aden',
                                year: 'numeric',
                                month: '2-digit',
                                day: '2-digit',
                                hour: '2-digit',
                                minute: '2-digit',
                                second: '2-digit',
                                hour12: false
                            }).replace(',', '');
                        } catch (e) {
                            // fallback: YYYY-MM-DD HH:mm:ss +03:00
                            const pad = n => n.toString().padStart(2, '0');
                            return `${date.getFullYear()}-${pad(date.getMonth()+1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())} +03:00`;
                        }
                    }
                    const lastModifiedStr = formatYemenDate(lastModified);
                    
                    sessionsInfo.push({
                        sessionId,
                        isActive: !!sock,
                        isConnected: sock && sock.user ? true : false,
                        hasQR: !!lastQRCodes[sessionId],
                        sessionSizeMB: (sessionSize / (1024 * 1024)).toFixed(2),
                        lastModified: lastModifiedStr,
                        lastModifiedRaw: lastModified,
                        reconnectAttempts: reconnectAttempts.get(sessionId) || 0,
                        pendingMessages: (pendingMessages.get(sessionId) || []).length
                    });
                } catch (sessionError) {
                    console.error(`Error processing session ${sessionId}:`, sessionError);
                    // إضافة معلومات أساسية حتى لو فشل في الحصول على التفاصيل
                    sessionsInfo.push({
                        sessionId,
                        isActive: false,
                        isConnected: false,
                        hasQR: false,
                        sessionSizeMB: '0.00',
                        lastModified: 'غير متوفر',
                        reconnectAttempts: 0,
                        pendingMessages: 0,
                        error: 'خطأ في قراءة الجلسة'
                    });
                }
            }
        }
        
        sessionsInfo.sort((a, b) => b.lastModifiedRaw - a.lastModifiedRaw);
        
        const totalSessions = sessionsInfo.length;
        const activeSessionsCount = sessionsInfo.filter(s => s.isActive).length;
        const connectedSessions = sessionsInfo.filter(s => s.isConnected).length;
        
        // حساب استهلاك الذاكرة للعملية
        const mem = process.memoryUsage();
        const rssMB = (mem.rss / 1024 / 1024).toFixed(2);
        const heapUsedMB = (mem.heapUsed / 1024 / 1024).toFixed(2);
        
        res.send(`
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>لوحة تحكم جلسات WhatsApp</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f5f5f5; }
                    .container { max-width: 1200px; margin: 0 auto; padding: 20px; }
                    .header { background: #128C7E; color: white; padding: 20px; border-radius: 10px; margin-bottom: 20px; }
                    .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin-bottom: 20px; }
                    .stat-card { background: white; padding: 20px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); text-align: center; }
                    .stat-number { font-size: 2em; font-weight: bold; color: #128C7E; }
                    .stat-label { color: #666; margin-top: 5px; }
                    .sessions-grid { display: grid; gap: 15px; }
                    .session-card { background: white; padding: 20px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .session-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
                    .session-id { font-weight: bold; font-size: 1.2em; color: #333; }
                    .status { padding: 5px 10px; border-radius: 5px; font-size: 0.9em; font-weight: bold; }
                    .status.connected { background: #d4edda; color: #155724; }
                    .status.disconnected { background: #f8d7da; color: #721c24; }
                    .status.inactive { background: #fff3cd; color: #856404; }
                    .status.error { background: #f8d7da; color: #721c24; }
                    .session-details { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; }
                    .detail { text-align: center; }
                    .detail-label { font-size: 0.9em; color: #666; margin-bottom: 5px; }
                    .detail-value { font-weight: bold; color: #333; }
                    .session-actions { display: flex; gap: 10px; margin-top: 15px; justify-content: center; }
                    .action-btn { 
                        padding: 8px 16px; 
                        border: none; 
                        border-radius: 5px; 
                        cursor: pointer; 
                        font-size: 0.9em; 
                        font-weight: bold;
                        transition: all 0.3s ease;
                    }
                    .delete-btn { 
                        background: #dc3545; 
                        color: white; 
                    }
                    .delete-btn:hover { background: #c82333; }
                    .qr-btn { 
                        background: #17a2b8; 
                        color: white; 
                    }
                    .qr-btn:hover { background: #138496; }
                    .refresh-btn { background: #128C7E; color: white; border: none; padding: 10px 20px; border-radius: 5px; cursor: pointer; margin-bottom: 20px; }
                    .refresh-btn:hover { background: #0a6960; }
                    .no-sessions { text-align: center; color: #666; padding: 40px; }
                    .error-message { background: #f8d7da; color: #721c24; padding: 10px; border-radius: 5px; margin-bottom: 10px; }
                    .modal { 
                        display: none; 
                        position: fixed; 
                        z-index: 1000; 
                        left: 0; 
                        top: 0; 
                        width: 100%; 
                        height: 100%; 
                        background-color: rgba(0,0,0,0.5); 
                    }
                    .modal-content { 
                        background-color: white; 
                        margin: 15% auto; 
                        padding: 20px; 
                        border-radius: 10px; 
                        width: 80%; 
                        max-width: 400px; 
                        text-align: center; 
                    }
                    .modal-buttons { 
                        display: flex; 
                        gap: 10px; 
                        justify-content: center; 
                        margin-top: 20px; 
                    }
                    .modal-btn { 
                        padding: 10px 20px; 
                        border: none; 
                        border-radius: 5px; 
                        cursor: pointer; 
                        font-weight: bold; 
                    }
                    .confirm-btn { background: #dc3545; color: white; }
                    .cancel-btn { background: #6c757d; color: white; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📱 لوحة تحكم جلسات WhatsApp</h1>
                        <p>مراقبة وإدارة جميع الجلسات النشطة</p>
                    </div>
                    
                    <div class="stats">
                        <div class="stat-card" style="background:#e3f2fd;">
                            <div class="stat-number">${rssMB} MB</div>
                            <div class="stat-label">RAM المستخدمة (RSS)</div>
                        </div>
                        <div class="stat-card" style="background:#e3f2fd;">
                            <div class="stat-number">${heapUsedMB} MB</div>
                            <div class="stat-label">Heap مستخدم فعليًا</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-number">${totalSessions}</div>
                            <div class="stat-label">إجمالي الجلسات</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-number">${activeSessionsCount}</div>
                            <div class="stat-label">الجلسات النشطة</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-number">${connectedSessions}</div>
                            <div class="stat-label">الجلسات المتصلة</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-number">${activeSessionsCount - connectedSessions}</div>
                            <div class="stat-label">الجلسات المقطوعة</div>
                        </div>
                    </div>
                    
                    <div class="sessions-grid">
                        ${sessionsInfo.length === 0 ? 
                            '<div class="no-sessions"><h3>لا توجد جلسات</h3><p>لم يتم إنشاء أي جلسات بعد</p></div>' :
                            sessionsInfo.map(session => `
                                <div class="session-card">
                                    <div class="session-header">
                                        <div class="session-id">${session.sessionId}</div>
                                        <div class="status ${session.error ? 'error' : session.isConnected ? 'connected' : session.isActive ? 'disconnected' : 'inactive'}">
                                            ${session.error ? '⚠️ خطأ' : session.isConnected ? '✅ متصل' : session.isActive ? '❌ مقطوع' : '⏸️ غير نشط'}
                                        </div>
                                    </div>
                                    ${session.error ? `<div class="error-message">${session.error}</div>` : ''}
                                    <div class="session-details">
                                        <div class="detail">
                                            <div class="detail-label">الحجم</div>
                                            <div class="detail-value">${session.sessionSizeMB} MB</div>
                                        </div>
                                        <div class="detail">
                                            <div class="detail-label">آخر تعديل</div>
                                            <div class="detail-value">${session.lastModified}</div>
                                        </div>
                                        <div class="detail">
                                            <div class="detail-label">محاولات إعادة الاتصال</div>
                                            <div class="detail-value">${session.reconnectAttempts}</div>
                                        </div>
                                        <div class="detail">
                                            <div class="detail-label">الرسائل المعلقة</div>
                                            <div class="detail-value">${session.pendingMessages}</div>
                                        </div>
                                        <div class="detail">
                                            <div class="detail-label">رمز QR</div>
                                            <div class="detail-value">${session.hasQR ? '✅ متوفر' : '❌ غير متوفر'}</div>
                                        </div>
                                    </div>
                                    <div class="session-actions">
                                        <button class="delete-btn action-btn" onclick="deleteSession('${session.sessionId}')">حذف</button>
                                        <button class="qr-btn action-btn" onclick="getQR('${session.sessionId}')">رمز QR</button>
                                    </div>
                                </div>
                            `).join('')
                        }
                    </div>
                </div>
                
                <div class="modal" id="deleteModal">
                    <div class="modal-content">
                        <h2>حذف الجلسة</h2>
                        <p>هل أنت متأكد من رغبتك في حذف هذه الجلسة؟</p>
                        <p id="sessionIdLabel" style="color:#128C7E;font-weight:bold;"></p>
                        <input type="hidden" id="sessionIdToDelete" />
                        <div class="modal-buttons">
                            <button class="confirm-btn modal-btn" onclick="confirmDelete()">تأكيد</button>
                            <button class="cancel-btn modal-btn" onclick="closeDeleteModal()">إلغاء</button>
                        </div>
                    </div>
                </div>
                
                <div class="modal" id="qrModal">
                    <div class="modal-content">
                        <h2>رمز QR</h2>
                        <div id="qrCodeModal"></div>
                        <button class="close-btn modal-btn" onclick="closeQRModal()">إغلاق</button>
                    </div>
                </div>
                
                <script>
                    // تحديث تلقائي كل 30 ثانية
                    setInterval(() => {
                        location.reload();
                    }, 30000);

                    // إغلاق النوافذ المنبثقة عند النقر خارجها
                    window.onclick = function(event) {
                        const deleteModal = document.getElementById('deleteModal');
                        const qrModal = document.getElementById('qrModal');
                        if (event.target === deleteModal) {
                            closeDeleteModal();
                        }
                        if (event.target === qrModal) {
                            closeQRModal();
                        }
                    }

                    function deleteSession(sessionId) {
                        document.getElementById('sessionIdToDelete').value = sessionId;
                        document.getElementById('sessionIdLabel').innerText = 'معرف الجلسة: ' + sessionId;
                        document.getElementById('deleteModal').style.display = 'block';
                    }

                    function confirmDelete() {
                        const sessionId = document.getElementById('sessionIdToDelete').value;
                        fetch('/delete_session/' + sessionId, { method: 'POST' })
                            .then(response => response.json())
                            .then(data => {
                                if (data.success) {
                                    alert('تم حذف الجلسة بنجاح!');
                                    location.reload();
                                } else {
                                    alert('خطأ في حذف الجلسة: ' + data.error);
                                }
                            })
                            .catch(error => {
                                alert('خطأ في الاتصال: ' + error.message);
                            })
                            .finally(() => {
                                closeDeleteModal();
                            });
                    }

                    function closeDeleteModal() {
                        document.getElementById('deleteModal').style.display = 'none';
                    }

                    function getQR(sessionId) {
                        // فتح رمز QR في نافذة جديدة
                        window.open('/qr/' + sessionId, '_blank');
                    }

                    function closeQRModal() {
                        document.getElementById('qrModal').style.display = 'none';
                    }
                </script>
            </body>
            </html>
        `);
        
    } catch (error) {
        console.error('Error generating sessions dashboard:', error);
        res.status(500).send(`
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
                <meta charset="UTF-8">
                <title>خطأ في لوحة التحكم</title>
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }
                    .error { background: #f8d7da; color: #721c24; padding: 20px; border-radius: 10px; margin: 20px; }
                </style>
            </head>
            <body>
                <h1>⚠️ خطأ في لوحة التحكم</h1>
                <div class="error">
                    <h3>حدث خطأ أثناء تحميل لوحة التحكم</h3>
                    <p>الخطأ: ${error.message}</p>
                    <button onclick="location.reload()">🔄 إعادة المحاولة</button>
                </div>
            </body>
            </html>
        `);
    }
});

// --- تم التعليق مؤقتًا لمنع الفحص الدوري للجلسات غير النشطة ---
// setInterval(closeInactiveSessions, 60 * 1000);
// --- نهاية التعليق ---

const PORT = process.env.PORT || 3002;
app.listen(PORT, () => {
    console.log(`WhatsApp server running on port ${PORT}`);
}); 