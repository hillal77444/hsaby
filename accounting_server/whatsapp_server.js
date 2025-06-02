const express = require('express');
const { Client, LocalAuth, MessageMedia } = require('whatsapp-web.js');
const qrcode = require('qrcode');
const path = require('path');
const fs = require('fs');
const fetch = require('node-fetch');
const mime = require('mime-types');
const multer = require('multer');
const cors = require('cors');

const app = express();
const port = 3003;
const host = process.env.HOST || 'localhost';
const LOG_FILE = path.join(__dirname, 'whatsapp_api.log');
const SESSION_FILE = path.join(__dirname, 'whatsapp_sessions.json');
const AUTH_DIR = path.join(__dirname, '.wwebjs_auth');
const UPLOAD_DIR = path.join(__dirname, 'uploads');
const FLASK_SERVER = process.env.FLASK_SERVER || 'http://localhost:5007';

// إعداد CORS
app.use(cors({
  origin: [FLASK_SERVER, 'http://localhost:5007', 'http://212.224.88.122:5007'],
  methods: ['GET', 'POST', 'DELETE'],
  allowedHeaders: ['Content-Type']
}));

// إعداد السجلات
function log(message) {
  const timestamp = new Date().toISOString();
  const logMessage = `[${timestamp}] ${message}\n`;
  console.log(logMessage);
  fs.appendFileSync(LOG_FILE, logMessage);
}

// إنشاء المجلدات المطلوبة
function ensureDirectories() {
  try {
    if (!fs.existsSync(AUTH_DIR)) {
      fs.mkdirSync(AUTH_DIR, { recursive: true });
      log('تم إنشاء مجلد الجلسات');
    }
    
    if (!fs.existsSync(UPLOAD_DIR)) {
      fs.mkdirSync(UPLOAD_DIR, { recursive: true });
      log('تم إنشاء مجلد الرفع');
    }
    
    if (!fs.existsSync(LOG_FILE)) {
      fs.writeFileSync(LOG_FILE, '');
      log('تم إنشاء ملف السجل');
    }
    
    if (!fs.existsSync(SESSION_FILE)) {
      fs.writeFileSync(SESSION_FILE, JSON.stringify({}));
      log('تم إنشاء ملف الجلسات');
    }
  } catch (err) {
    console.error('خطأ في إنشاء المجلدات:', err);
    process.exit(1);
  }
}

// إعداد multer للرفع
const upload = multer({ dest: UPLOAD_DIR });

app.use(express.json());
const sessions = new Map();

// دالة لإعداد مستمع الرسائل
function setupMessageListener(client, sessionId) {
  client.on('message', async (msg) => {
    try {
      if (msg.fromMe) return; // تجاهل الرسائل المرسلة منا
      
      const response = await fetch(`${FLASK_SERVER}/check_autoreply`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          session_id: sessionId,
          message_text: msg.body,
          sender_number: msg.from
        })
      });
      
      if (!response.ok) {
        log(`❌ فشل في فحص الرد التلقائي: ${await response.text()}`);
      }
    } catch (err) {
      log(`❌ خطأ في معالجة الرسالة الواردة: ${err.message}`);
    }
  });
}

// دالة محسنة لإنشاء الجلسات
async function createSession(id, retries = 3) {
  try {
    log(`بدء إنشاء جلسة جديدة: ${id} (المحاولة ${4 - retries}/3)`);

    // التحقق من وجود جلسة نشطة
    if (sessions.has(id) && sessions.get(id).ready) {
      log(`الجلسة ${id} موجودة بالفعل ونشطة`);
      return;
    }

    // تنظيف الجلسات القديمة
    cleanupOldSessions();

    // التأكد من وجود مجلد الجلسة
    const sessionDir = path.join(AUTH_DIR, id);
    if (!fs.existsSync(sessionDir)) {
      fs.mkdirSync(sessionDir, { recursive: true });
      log(`تم إنشاء مجلد الجلسة: ${sessionDir}`);
    }

    log(`جاري تهيئة عميل WhatsApp...`);
    const client = new Client({
      authStrategy: new LocalAuth({ 
        clientId: id,
        dataPath: path.join(AUTH_DIR, id)
      }),
      puppeteer: {
        headless: true,
        args: [
          '--no-sandbox',
          '--disable-setuid-sandbox',
          '--disable-dev-shm-usage',
          '--disable-accelerated-2d-canvas',
          '--no-first-run',
          '--no-zygote',
          '--disable-gpu',
          '--single-process'
        ],
        executablePath: process.env.CHROMIUM_PATH || undefined,
        ignoreHTTPSErrors: true,
        timeout: 120000
      },
      webVersionCache: {
        type: 'remote',
        remotePath: 'https://raw.githubusercontent.com/wppconnect-team/wa-version/main/html/2.2415.4.html'
      },
      qrMaxRetries: 5,
      authTimeoutMs: 120000,
      qrQualityOptions: {
        quality: 0.8,
        margin: 4
      },
      restartOnAuthFail: true,
      takeoverOnConflict: true,
      takeoverTimeoutMs: 60000
    });

    // إضافة معالج للأخطاء غير المتوقعة
    client.on('error', (error) => {
      log(`خطأ غير متوقع في الجلسة ${id}: ${error.message}`);
      if (retries > 0) {
        log(`محاولة إعادة إنشاء الجلسة ${id}`);
        setTimeout(() => createSession(id, retries - 1), 5000);
      }
    });

    client.on('loading_screen', (percent, message) => {
      log(`جاري التحميل: ${percent}% - ${message}`);
    });

    client.on('change_state', (state) => {
      log(`تغيير حالة الجلسة ${id}: ${state}`);
    });

    // إعداد معالجات الأحداث
    client.on('qr', async (qr) => {
      log(`تم إنشاء QR code للجلسة: ${id}`);
      try {
        const qrUrl = await qrcode.toDataURL(qr, {
          errorCorrectionLevel: 'H',
          margin: 1,
          width: 300
        });
        if (sessions.has(id)) {
          sessions.get(id).qr = qrUrl;
          sessions.get(id).lastUpdate = new Date();
          log(`تم حفظ QR code للجلسة: ${id}`);
        } else {
          log(`❌ الجلسة غير موجودة عند محاولة حفظ QR: ${id}`);
        }
      } catch (err) {
        log(`خطأ في تحويل QR: ${err.message}`);
      }
    });

    client.on('authenticated', () => {
      log(`تم المصادقة بنجاح للجلسة: ${id}`);
      if (sessions.has(id)) {
        sessions.get(id).authenticated = true;
        // تحديث الحالة في قاعدة البيانات
        fetch(`${FLASK_SERVER}/update_session_status`, {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({
            session_id: id,
            status: 'active',
            file_exists: true
          })
        }).catch(err => log(`خطأ في تحديث حالة المصادقة: ${err.message}`));
      }
    });

    client.on('ready', () => {
      if (sessions.has(id)) {
        sessions.get(id).ready = true;
        sessions.get(id).lastUpdate = new Date();
        log(`✅ الجلسة جاهزة: ${id}`);
      }
    });

    client.on('disconnected', (reason) => {
      if (sessions.has(id)) {
        sessions.get(id).ready = false;
        sessions.get(id).authenticated = false;
        log(`❌ تم قطع الاتصال بالجلسة: ${id}، السبب: ${reason}`);
        // تحديث الحالة في قاعدة البيانات
        fetch(`${FLASK_SERVER}/update_session_status`, {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({
            session_id: id,
            status: 'inactive',
            file_exists: false
          })
        }).catch(err => log(`خطأ في تحديث حالة الاتصال: ${err.message}`));
        
        if (reason === 'NAVIGATION_ERROR') {
          log(`إعادة تشغيل الجلسة تلقائياً: ${id}`);
          setTimeout(() => createSession(id), 5000);
        }
      }
    });

    client.on('auth_failure', (msg) => {
      log(`❌ فشل المصادقة للجلسة: ${id}، الرسالة: ${msg}`);
      if (sessions.has(id)) {
        sessions.get(id).authenticated = false;
        sessions.get(id).ready = false;
      }
    });

    sessions.set(id, { 
      client, 
      qr: null, 
      ready: false, 
      authenticated: false,
      lastUpdate: new Date() 
    });

    await client.initialize();
    
    // إعداد مستمع الرسائل
    setupMessageListener(client, id);

  } catch (err) {
    log(`❌ خطأ في إنشاء الجلسة: ${err.message}`);
    if (retries > 0) {
      log(`محاولة إعادة إنشاء الجلسة ${id}`);
      setTimeout(() => createSession(id, retries - 1), 5000);
    } else {
      log(`❌ فشل إنشاء الجلسة بعد عدة محاولات: ${id}`);
    }
  }
}

// دالة لتنظيف الجلسات القديمة
function cleanupOldSessions() {
  const now = Date.now();
  const MAX_SESSION_AGE = 24 * 60 * 60 * 1000; // 24 ساعة

  sessions.forEach((session, id) => {
    if (now - session.lastUpdate > MAX_SESSION_AGE) {
      log(`تنظيف الجلسة القديمة: ${id}`);
      if (session.client) {
        session.client.destroy();
      }
      sessions.delete(id);
    }
  });
}

// دالة لاستعادة الجلسات المحفوظة
async function restoreSessions() {
  try {
    const sessionDirs = fs.readdirSync(AUTH_DIR);
    log(`جاري استعادة ${sessionDirs.length} جلسة...`);

    for (const sessionId of sessionDirs) {
      if (sessionId === '.gitkeep') continue;
      
      const sessionPath = path.join(AUTH_DIR, sessionId);
      if (fs.statSync(sessionPath).isDirectory()) {
        log(`استعادة الجلسة: ${sessionId}`);
        await createSession(sessionId);
      }
    }
    
    log('✅ تم استعادة جميع الجلسات');
  } catch (err) {
    log(`❌ خطأ في استعادة الجلسات: ${err.message}`);
  }
}

// تحسين معالجة الإغلاق
let isShuttingDown = false;

async function gracefulShutdown() {
  if (isShuttingDown) return;
  isShuttingDown = true;
  
  log(`\n🛑 جاري إيقاف الخادم...`);
  
  try {
    // إغلاق جميع الجلسات
    const closePromises = [];
    for (const [id, session] of sessions) {
      if (session.client) {
        closePromises.push(
          session.client.destroy()
            .then(() => log(`✅ تم إغلاق جلسة ${id}`))
            .catch(err => log(`❌ خطأ في إغلاق جلسة ${id}: ${err.message}`))
        );
      }
    }
    
    // انتظار إغلاق جميع الجلسات
    await Promise.all(closePromises);
    
    // حذف الملفات المؤقتة
    try {
      const files = fs.readdirSync(UPLOAD_DIR);
      for (const file of files) {
        fs.unlinkSync(path.join(UPLOAD_DIR, file));
      }
      log('🧹 تم تنظيف الملفات المؤقتة');
    } catch (err) {
      log(`⚠️ خطأ في تنظيف الملفات المؤقتة: ${err.message}`);
    }
    
    log('✅ تم إيقاف الخادم بنجاح');
    process.exit(0);
  } catch (err) {
    log(`❌ خطأ في إيقاف الخادم: ${err.message}`);
    process.exit(1);
  }
}

// تحسين معالجة الإشارات
process.on('SIGINT', gracefulShutdown);
process.on('SIGTERM', gracefulShutdown);
process.on('uncaughtException', (err) => {
  log(`❌ خطأ غير متوقع: ${err.message}`);
  gracefulShutdown();
});
process.on('unhandledRejection', (reason, promise) => {
  log(`❌ وعد مرفوض غير معالج: ${reason}`);
  gracefulShutdown();
});

// تحسين بدء التشغيل
async function startServer() {
  try {
    ensureDirectories();
    
    // التحقق من وجود عمليات قديمة
    if (process.env.DEBUG === '1') {
      log('🔍 وضع التصحيح مفعل');
    }
    
    const server = app.listen(port, '0.0.0.0', async () => {
      log(`✅ API يعمل على ${host}:${port}`);
      log(`يمكن الوصول إلى الخادم عبر:`);
      log(`- http://localhost:${port}`);
      log(`- http://127.0.0.1:${port}`);
      log(`- http://212.224.88.122:${port}`);
      
      // استعادة الجلسات بعد بدء الخادم
      await restoreSessions();
      
      // إرسال إشارة جاهزية
      if (process.send) {
        process.send('ready');
      }
    });
    
    // معالجة أخطاء الخادم
    server.on('error', (err) => {
      if (err.code === 'EADDRINUSE') {
        log(`❌ المنفذ ${port} مشغول بالفعل`);
        process.exit(1);
      } else {
        log(`❌ خطأ في الخادم: ${err.message}`);
        process.exit(1);
      }
    });
    
  } catch (err) {
    log(`❌ فشل في بدء الخادم: ${err.message}`);
    process.exit(1);
  }
}

// بدء الخادم
startServer();

// مسارات API
app.get('/start/:sessionId', async (req, res) => {
  const id = req.params.sessionId;
  log(`طلب بدء جلسة: ${id}`);
  
  if (!sessions.has(id)) {
    await createSession(id);
    res.json({ 
      status: 'starting',
      message: 'جاري بدء الجلسة...',
      timestamp: new Date()
    });
  } else {
    const session = sessions.get(id);
    res.json({
      status: session.ready ? 'ready' : 'pending',
      qr_available: !!session.qr,
      last_update: session.lastUpdate
    });
  }
});

app.get('/qr/:sessionId', (req, res) => {
  const id = req.params.sessionId;
  log(`طلب QR code للجلسة: ${id}`);

  if (!sessions.has(id)) {
    log(`❌ الجلسة غير موجودة: ${id}`);
    return res.status(404).send(`
      <div style="text-align:center; padding:20px;">
        <h4 style="color:red;">❌ الجلسة غير موجودة</h4>
        <p>جاري محاولة إنشاء الجلسة تلقائياً...</p>
        <script>
          setTimeout(() => window.location.reload(), 3000);
        </script>
      </div>
    `);
  }

  const session = sessions.get(id);
  if (session.qr) {
    log(`✔ إرسال QR code للجلسة: ${id}`);
    return res.send(`
      <div style="text-align:center; padding:20px;">
        <h3 style="margin-bottom:20px;">🔒 امسح رمز QR لربط الواتساب</h3>
        <img src="${session.qr}" alt="QR Code" style="max-width:300px;"/>
        <p style="margin-top:20px;">تاريخ الإنشاء: ${session.lastUpdate.toLocaleString()}</p>
        <button onclick="window.location.reload()" style="margin-top:10px; padding:8px 15px; background:#4CAF50; color:white; border:none; border-radius:4px;">
          تحديث الصفحة
        </button>
      </div>
    `);
  }

  log(`⏳ QR code غير جاهز للجلسة: ${id}`);
  res.status(202).send(`
    <div style="text-align:center; padding:20px;">
      <h4 style="color:orange;">⏳ رمز QR غير جاهز بعد</h4>
      <p>حالة الجلسة: ${session.ready ? 'جاهزة' : 'في انتظار QR'}</p>
      <p>آخر تحديث: ${session.lastUpdate.toLocaleString()}</p>
      <button onclick="window.location.reload()" style="margin-top:10px; padding:8px 15px; background:#2196F3; color:white; border:none; border-radius:4px;">
        إعادة تحميل
      </button>
    </div>
  `);
});

app.post('/send/:sessionId', upload.single('image'), async (req, res) => {
  const { numbers, message } = req.body;
  const id = req.params.sessionId;

  log(`طلب إرسال رسالة إلى ${numbers.length} رقم عبر الجلسة: ${id}`);

  if (!sessions.has(id)) {
    return res.status(404).json({ error: 'الجلسة غير موجودة' });
  }

  const session = sessions.get(id);
  if (!session.ready) {
    return res.status(400).json({ status: 'not_ready' });
  }

  const results = [];
  const errors = [];

  for (const number of numbers) {
    try {
      // تنظيف وتنسيق رقم الهاتف
      let cleanNumber = number.replace(/[^0-9]/g, '');
      if (!cleanNumber) {
        errors.push({ number, error: 'رقم الهاتف غير صالح' });
        continue;
      }

      // إزالة الصفر من بداية الرقم إذا كان موجوداً
      if (cleanNumber.startsWith('0')) {
        cleanNumber = cleanNumber.substring(1);
      }

      const chatId = `${cleanNumber}@c.us`;
      log(`إرسال رسالة إلى: ${chatId}`);

      if (req.file) {
        try {
          // الحصول على امتداد الملف الأصلي
          const originalExt = path.extname(req.file.originalname).toLowerCase();
          const newPath = `${req.file.path}${originalExt}`;
          
          // إعادة تسمية الملف مع الاحتفاظ بالامتداد الأصلي
          fs.renameSync(req.file.path, newPath);

          // تحديد نوع MIME بناءً على الامتداد
          const mimeType = mime.lookup(originalExt) || 'application/octet-stream';
          const media = MessageMedia.fromFilePath(newPath);
          media.mimetype = mimeType;

          // إرسال الملف بناءً على نوعه
          if (mimeType.startsWith('image/')) {
            await session.client.sendMessage(chatId, media, { 
              caption: message || '',
              sendMediaAsDocument: false
            });
            log(`✔ تم إرسال الصورة إلى ${cleanNumber}`);
          } else if (mimeType.startsWith('video/')) {
            await session.client.sendMessage(chatId, media, { 
              caption: message || '',
              sendMediaAsDocument: false
            });
            log(`✔ تم إرسال الفيديو إلى ${cleanNumber}`);
          } else if (mimeType.startsWith('audio/')) {
            await session.client.sendMessage(chatId, media, { 
              caption: message || '',
              sendMediaAsDocument: false
            });
            log(`✔ تم إرسال الصوت إلى ${cleanNumber}`);
          } else {
            await session.client.sendMessage(chatId, media, { 
              caption: message || '',
              sendMediaAsDocument: true
            });
            log(`✔ تم إرسال الملف كمستند إلى ${cleanNumber}`);
          }

          // حذف الملف بعد الإرسال
          fs.unlink(newPath, (err) => {
            if (err) {
              log(`⚠️ فشل حذف الملف: ${newPath}, الخطأ: ${err.message}`);
            } else {
              log(`🧹 تم حذف الملف المؤقت: ${newPath}`);
            }
          });
        } catch (err) {
          log(`❌ خطأ في معالجة الملف: ${err.message}`);
          errors.push({ number: cleanNumber, error: `فشل في معالجة الملف: ${err.message}` });
          continue;
        }
      } else {
        // إرسال رسالة نصية إذا لم يكن هناك ملف
        if (!message) {
          errors.push({ number: cleanNumber, error: 'الرسالة مطلوبة' });
          continue;
        }
        await session.client.sendMessage(chatId, message);
        log(`✔ تم إرسال الرسالة النصية إلى ${cleanNumber}`);
      }

      results.push({
        number: cleanNumber,
        status: 'sent',
        timestamp: new Date().toISOString()
      });

    } catch (err) {
      log(`❌ فشل الإرسال إلى ${number}: ${err.message}`);
      errors.push({ number, error: err.message });
    }
  }

  res.json({
    status: errors.length === 0 ? 'success' : 'partial',
    results,
    errors,
    timestamp: new Date().toISOString()
  });
});

app.get('/status', (req, res) => {
  const status = {
    status: 'running',
    timestamp: new Date().toISOString(),
    sessions: Array.from(sessions.entries()).map(([id, session]) => ({
      id,
      ready: session.ready,
      authenticated: session.authenticated,
      qr_available: !!session.qr,
      last_update: session.lastUpdate,
      memory_usage: process.memoryUsage()
    }))
  };
  res.json(status);
});

// إغلاق جميع الجلسات
app.post('/close-all-sessions', async (req, res) => {
  try {
    const sessionList = Array.from(sessions.entries());
    const results = [];
    
    for (const [id, session] of sessionList) {
      try {
        if (session.client) {
          await session.client.destroy();
          log(`تم إغلاق جلسة ${id}`);
        }
        sessions.delete(id);
        results.push({
          id,
          status: 'closed',
          message: 'تم إغلاق الجلسة بنجاح'
        });
      } catch (error) {
        log(`خطأ في إغلاق جلسة ${id}: ${error.message}`);
        results.push({
          id,
          status: 'error',
          message: error.message
        });
      }
    }
    
    res.json({
      status: 'success',
      message: 'تم محاولة إغلاق جميع الجلسات',
      results: results
    });
  } catch (error) {
    log(`خطأ في إغلاق جميع الجلسات: ${error.message}`);
    res.status(500).json({
      status: 'error',
      message: error.message
    });
  }
});

// إيقاف الخادم
app.post('/stop-server', async (req, res) => {
  try {
    // إغلاق جميع الجلسات أولاً
    const sessionList = Array.from(sessions.entries());
    for (const [id, session] of sessionList) {
      try {
        if (session.client) {
          await session.client.destroy();
          log(`تم إغلاق جلسة ${id}`);
        }
        sessions.delete(id);
      } catch (error) {
        log(`خطأ في إغلاق جلسة ${id}: ${error.message}`);
      }
    }
    
    res.json({
      status: 'success',
      message: 'تم إيقاف الخادم بنجاح'
    });
    
    // إيقاف الخادم بعد إرسال الرد
    setTimeout(() => {
      process.exit(0);
    }, 1000);
    
  } catch (error) {
    log(`خطأ في إيقاف الخادم: ${error.message}`);
    res.status(500).json({
      status: 'error',
      message: error.message
    });
  }
});

// إعادة تشغيل الخادم
app.post('/restart-server', async (req, res) => {
  try {
    // إغلاق جميع الجلسات أولاً
    const sessionList = Array.from(sessions.entries());
    for (const [id, session] of sessionList) {
      try {
        if (session.client) {
          await session.client.destroy();
          log(`تم إغلاق جلسة ${id}`);
        }
        sessions.delete(id);
      } catch (error) {
        log(`خطأ في إغلاق جلسة ${id}: ${error.message}`);
      }
    }
    
    res.json({
      status: 'success',
      message: 'سيتم إعادة تشغيل الخادم'
    });
    
    // إعادة تشغيل الخادم بعد إرسال الرد
    setTimeout(() => {
      process.exit(0);
    }, 1000);
    
  } catch (error) {
    log(`خطأ في إعادة تشغيل الخادم: ${error.message}`);
    res.status(500).json({
      status: 'error',
      message: error.message
    });
  }
});

// حذف جلسة محددة
app.delete('/delete/:sessionId', async (req, res) => {
  const id = req.params.sessionId;
  log(`طلب حذف الجلسة: ${id}`);
  
  if (sessions.has(id)) {
    try {
      const session = sessions.get(id);
      if (session.client) {
        await session.client.destroy();
        log(`تم إغلاق جلسة ${id}`);
      }
      sessions.delete(id);
      res.json({ 
        status: 'success',
        message: 'تم حذف الجلسة بنجاح',
        id 
      });
    } catch (error) {
      log(`خطأ في حذف الجلسة ${id}: ${error.message}`);
      res.status(500).json({ 
        status: 'error',
        message: error.message 
      });
    }
  } else {
    res.status(404).json({ 
      status: 'error',
      message: 'الجلسة غير موجودة' 
    });
  }
});

app.delete('/reset/:sessionId', async (req, res) => {
  const id = req.params.sessionId;
  log(`طلب إعادة تعيين الجلسة: ${id}`);
  
  if (sessions.has(id)) {
    try {
      await sessions.get(id).client.destroy();
      log(`✔ تم تدمير عميل الجلسة: ${id}`);
    } catch (err) {
      log(`⚠ تحذير أثناء تدمير العميل: ${err.message}`);
    }
    sessions.delete(id);
    res.json({ status: 'session_deleted', id });
  } else {
    res.status(404).json({ error: 'الجلسة غير موجودة' });
  }
});