const { default: makeWASocket, useMultiFileAuthState, DisconnectReason } = require('@whiskeysockets/baileys');
const { Boom } = require('@hapi/boom');
const express = require('express');
const cors = require('cors');
const pino = require('pino');
const qrcode = require('qrcode-terminal');

const app = express();
app.use(cors());
app.use(express.json());

// تخزين الجلسات النشطة
const activeSessions = new Map();

// دالة إنشاء جلسة واتساب
async function createWhatsAppSession(sessionId) {
    try {
        const { state, saveCreds } = await useMultiFileAuthState(`sessions/${sessionId}`);
        
        const sock = makeWASocket({
            printQRInTerminal: true,
            auth: state,
            logger: pino({ level: 'silent' }),
            browser: ['Chrome (Linux)', '', ''],
            connectTimeoutMs: 60000,
            defaultQueryTimeoutMs: 60000,
            emitOwnEvents: false,
            markOnlineOnConnect: false
        });

        // معالجة تحديثات الاتصال
        sock.ev.on('connection.update', async (update) => {
            const { connection, lastDisconnect, qr } = update;
            
            if (qr) {
                qrcode.generate(qr, { small: true });
                sock.qr = qr;
            }
            
            if (connection === 'close') {
                const shouldReconnect = (lastDisconnect?.error instanceof Boom)?.output?.statusCode !== DisconnectReason.loggedOut;
                
                if (shouldReconnect) {
                    createWhatsAppSession(sessionId);
                } else {
                    activeSessions.delete(sessionId);
                }
            }
        });

        // حفظ بيانات الاعتماد
        sock.ev.on('creds.update', saveCreds);

        // تخزين الجلسة
        activeSessions.set(sessionId, sock);
        
        return sock;
    } catch (error) {
        console.error('Error creating session:', error);
        throw error;
    }
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
        
        const sock = activeSessions.get(sessionId);
        if (!sock) {
            return res.status(404).json({ success: false, error: 'Session not found' });
        }

        // تنسيق الرقم
        const formattedNumber = number.replace(/[^0-9]/g, '');
        const fullNumber = formattedNumber.startsWith('967') ? formattedNumber : `967${formattedNumber}`;

        await sock.sendMessage(`${fullNumber}@s.whatsapp.net`, { text: message });
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
                const fullNumber = formattedNumber.startsWith('967') ? formattedNumber : `967${formattedNumber}`;
                
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
    try {
        const sock = await createWhatsAppSession(sessionId);
        res.send(`
            <html>
                <head>
                    <title>WhatsApp QR Code</title>
                    <style>
                        body { display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background: #f0f0f0; }
                        .container { text-align: center; background: white; padding: 20px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }
                        h2 { color: #128C7E; }
                        #qrcode { margin: 20px 0; }
                    </style>
                    <script src="https://cdn.jsdelivr.net/npm/qrcode@1.5.1/build/qrcode.min.js"></script>
                </head>
                <body>
                    <div class="container">
                        <h2>WhatsApp QR Code</h2>
                        <div id="qrcode"></div>
                        <p>Scan this QR code with your WhatsApp</p>
                    </div>
                    <script>
                        // تحديث رمز QR كل 5 ثواني
                        function updateQR() {
                            fetch('/qr_data/${sessionId}')
                                .then(response => response.json())
                                .then(data => {
                                    if (data.qr) {
                                        QRCode.toCanvas(document.getElementById('qrcode'), data.qr, {
                                            width: 300,
                                            margin: 1,
                                            color: {
                                                dark: '#128C7E',
                                                light: '#ffffff'
                                            }
                                        });
                                    }
                                });
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
    const sock = activeSessions.get(sessionId);
    if (sock && sock.qr) {
        res.json({ qr: sock.qr });
    } else {
        res.json({ qr: null });
    }
});

const PORT = process.env.PORT || 3002;
app.listen(PORT, () => {
    console.log(`WhatsApp server running on port ${PORT}`);
}); 