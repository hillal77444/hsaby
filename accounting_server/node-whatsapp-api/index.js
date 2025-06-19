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

// تخزين الجلسات النشطة
const activeSessions = new Map();
const lastQRCodes = {};
const sessionTimeouts = new Map();

// دالة إنشاء جلسة واتساب
async function createWhatsAppSession(sessionId) {
    try {
        // console.log('createWhatsAppSession CALLED for:', sessionId);
        const { state, saveCreds } = await useMultiFileAuthState(`sessions/${sessionId}`);
        // console.log('useMultiFileAuthState DONE for:', sessionId);
        
        const sock = makeWASocket({
            printQRInTerminal: true,
            auth: state,
            logger: pino({ level: 'debug' }), // مستوى السجل debug
            browser: ['Chrome (Linux)', '', ''],
            connectTimeoutMs: 60000,
            defaultQueryTimeoutMs: 60000,
            emitOwnEvents: false,
            markOnlineOnConnect: false
        });
        // console.log('makeWASocket DONE for:', sessionId);

        // معالجة تحديثات الاتصال
        sock.ev.on('connection.update', async (update) => {
            // console.log('connection.update:', JSON.stringify(update));
            const { connection, lastDisconnect, qr } = update;
            
            if (qr) {
                // console.log('QR RECEIVED for', sessionId);
                qrcode.generate(qr, { small: true });
                lastQRCodes[sessionId] = qr; // حفظ رمز QR في متغير عام
            }
            if (connection === 'open') {
                // لا تحذف lastQRCodes[sessionId] حتى يبقى رمز QR متاحاً للعرض في المتصفح
                // console.log('WhatsApp CONNECTED for', sessionId);
            }
            if (connection === 'close') {
                const shouldReconnect = (lastDisconnect?.error instanceof Boom)?.output?.statusCode !== DisconnectReason.loggedOut;
                // console.log('WhatsApp CONNECTION CLOSED for', sessionId, 'shouldReconnect:', shouldReconnect);
                if (shouldReconnect) {
                    createWhatsAppSession(sessionId);
                } else {
                    activeSessions.delete(sessionId);
                }
            }
        });

        // حفظ بيانات الاعتماد
        sock.ev.on('creds.update', saveCreds);
        // console.log('sock.ev CREDS.UPDATE registered for:', sessionId);

        // تخزين الجلسة
        activeSessions.set(sessionId, sock);
        // console.log('activeSessions SET for:', sessionId);
        
        return sock;
    } catch (error) {
        console.error('Error creating session:', error);
        throw error;
    }
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

function scheduleSessionClose(sessionId, minutes = 10) {
    if (sessionTimeouts.has(sessionId)) {
        clearTimeout(sessionTimeouts.get(sessionId));
    }
    const timeout = setTimeout(() => {
        const sock = activeSessions.get(sessionId);
        if (sock) {
            sock.end();
            activeSessions.delete(sessionId);
            sessionTimeouts.delete(sessionId);
            // تنظيف الملفات المؤقتة فقط
            cleanSessionTempFiles(sessionId);
            console.log(`✅ Session ${sessionId} closed after ${minutes} minutes of inactivity.`);
        }
    }, minutes * 60 * 1000);
    sessionTimeouts.set(sessionId, timeout);
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
    try {
        const { sessionId } = req.params;
        const { number, message } = req.body;

        let sock = activeSessions.get(sessionId);
        if (!sock) {
            sock = await createWhatsAppSession(sessionId);
        }

        // أرسل الرسالة
        const formattedNumber = number.replace(/[^0-9]/g, '');
        const fullNumber = (formattedNumber.startsWith('967') || formattedNumber.startsWith('966'))
            ? formattedNumber
            : `967${formattedNumber}`;
        await sock.sendMessage(`${fullNumber}@s.whatsapp.net`, { text: message });
        

        // جدولة إغلاق الجلسة بعد 10 دقائق من آخر استخدام
        scheduleSessionClose(sessionId, 10);

        res.json({ success: true });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

// إرسال رسائل جماعية
app.post('/send_bulk/:sessionId', async (req, res) => {
    try {
        const { sessionId } = req.params;
        const { numbers, message, delay = 1000 } = req.body;
        
        const sock = activeSessions.get(sessionId);
        if (!sock) {
            return res.status(404).json({ success: false, error: 'Session not found' });
        }

        const results = [];
        for (const number of numbers) {
            try {
                const formattedNumber = number.replace(/[^0-9]/g, '');
                const fullNumber = (formattedNumber.startsWith('967') || formattedNumber.startsWith('966'))
                    ? formattedNumber
                    : `967${formattedNumber}`;
                
                await sock.sendMessage(`${fullNumber}@s.whatsapp.net`, { text: message });
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
    let sessionExists = false;
    try {
        if (fs.existsSync(credsFile)) {
            // الجلسة موجودة
            sessionExists = true;
            await createWhatsAppSession(sessionId);
        } else {
            // الجلسة غير موجودة، أنشئ جلسة جديدة (سيتم توليد QR)
            await createWhatsAppSession(sessionId);
        }
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
                        #info { color: #128C7E; margin-bottom: 10px; }
                    </style>
                    <script src="https://cdn.jsdelivr.net/npm/qrcode@1.5.1/build/qrcode.min.js"></script>
                </head>
                <body>
                    <div class="container">
                        <h2>WhatsApp QR Code</h2>
                        <div id="info">
                            ${sessionExists ? 'الجلسة موجودة بالفعل وتم تحميلها. لا حاجة لمسح رمز QR.' : 'يرجى مسح رمز QR لتسجيل الدخول.'}
                        </div>
                        <div id="qrcode"></div>
                        <button onclick="updateQR()">تحديث الكود يدويًا</button>
                        <p>Scan this QR code with your WhatsApp</p>
                        <div id="error"></div>
                    </div>
                    <script>
                        function showError(msg) {
                            document.getElementById('error').innerText = msg;
                        }
                        async function updateQR() {
                            try {
                                const response = await fetch('/qr_data/${sessionId}');
                                const data = await response.json();
                                if (data.qr) {
                                    if (typeof QRCode === 'undefined') {
                                        showError('مكتبة QRCode لم تُحمّل بشكل صحيح!');
                                        return;
                                    }
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
                                        if (error) showError('حدث خطأ أثناء رسم الكود: ' + error);
                                    });
                                } else {
                                    showError('لا يوجد رمز QR متاح حاليًا.');
                                }
                            } catch (e) {
                                showError('خطأ في جلب رمز QR: ' + e);
                            }
                        }
                        updateQR();
                        setInterval(updateQR, 5000);
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
    res.json({ qr: lastQRCodes[sessionId] || null });
});

function getSessionSize(sessionId) {
    const sessionDir = path.join(__dirname, 'sessions', sessionId);
    let totalSize = 0;
    function getSize(dir) {
        if (fs.existsSync(dir)) {
            fs.readdirSync(dir).forEach(file => {
                const filePath = path.join(dir, file);
                const stat = fs.statSync(filePath);
                if (stat.isDirectory()) {
                    getSize(filePath);
                } else {
                    totalSize += stat.size;
                }
            });
        }
    }
    getSize(sessionDir);
    return totalSize; // بالبايت
}

app.get('/session_size/:sessionId', (req, res) => {
    const { sessionId } = req.params;
    const size = getSessionSize(sessionId);
    res.json({ sessionId, sizeBytes: size, sizeMB: (size / (1024 * 1024)).toFixed(2) });
});

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
        createWhatsAppSession(sessionId).then(() => {
            console.log('✅ WhatsApp session (admin_main) created and started automatically.');
        }).catch((err) => {
            console.error('❌ Failed to start WhatsApp session automatically:', err.message);
        });
    } else {
        // توجد جلسة، شغّل الجلسة فقط
        createWhatsAppSession(sessionId).then(() => {
            console.log('✅ WhatsApp session (admin_main) loaded and started automatically.');
        }).catch((err) => {
            console.error('❌ Failed to load WhatsApp session automatically:', err.message);
        });
    }
});

const PORT = process.env.PORT || 3002;
app.listen(PORT, () => {
    console.log(`WhatsApp server running on port ${PORT}`);
}); 