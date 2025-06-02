from app import create_app
import subprocess
import os
import signal
import sys
import logging
import time
import platform
import requests

# إعداد السجلات
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('server.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# تحديد المسارات
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
NODE_DIR = BASE_DIR
WHATSAPP_SERVER = os.path.join(NODE_DIR, 'whatsapp_server.js')

def is_whatsapp_server_running():
    try:
        response = requests.get('http://localhost:3003/status', timeout=10)
        return response.status_code == 200
    except requests.exceptions.RequestException as e:
        logger.debug(f"فشل الاتصال بخادم الواتساب: {str(e)}")
        return False

def kill_existing_processes():
    try:
        # محاولة إغلاق جميع صفحات الواتساب أولاً
        try:
            response = requests.post('http://localhost:3003/close-all-sessions', timeout=5)
            if response.status_code == 200:
                logger.info("تم إغلاق جميع جلسات الواتساب")
        except Exception as e:
            logger.debug(f"لا يمكن الاتصال بخادم الواتساب: {str(e)}")

        # إيقاف عمليات Node.js
        if platform.system() == 'Windows':
            subprocess.run(['taskkill', '/F', '/IM', 'node.exe'], 
                         stdout=subprocess.PIPE, 
                         stderr=subprocess.PIPE)
        else:
            subprocess.run(['pkill', '-f', 'whatsapp_server.js'],
                         stdout=subprocess.PIPE,
                         stderr=subprocess.PIPE)
        
        logger.info("تم إيقاف جميع العمليات القديمة")
        time.sleep(3)  # زيادة وقت الانتظار للتأكد من إغلاق جميع العمليات
    except Exception as e:
        logger.error(f"خطأ في إيقاف العمليات القديمة: {str(e)}")

def start_node_server():
    try:
        # إيقاف العمليات القديمة
        kill_existing_processes()

        # التحقق من وجود الملف
        if not os.path.exists(WHATSAPP_SERVER):
            raise Exception(f"ملف whatsapp_server.js غير موجود في: {WHATSAPP_SERVER}")
        
        # التحقق من تثبيت Node.js
        try:
            subprocess.run(['node', '--version'], 
                         stdout=subprocess.PIPE, 
                         stderr=subprocess.PIPE, 
                         check=True)
        except subprocess.CalledProcessError:
            raise Exception("Node.js غير مثبت أو غير متاح")
        
        # تشغيل خادم WhatsApp
        logger.info("بدء تشغيل خادم WhatsApp...")
        process = subprocess.Popen(
            ['node', 'whatsapp_server.js'],
            cwd=NODE_DIR,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            env={**os.environ, 'DEBUG': '1', 'HOST': 'localhost'},
            text=True
        )
        
        # انتظار بدء الخادم
        max_retries = 15  # زيادة عدد المحاولات
        retry_count = 0
        while retry_count < max_retries:
            if is_whatsapp_server_running():
                logger.info("✅ تم تشغيل خادم WhatsApp بنجاح")
                return process
            time.sleep(3)  # زيادة وقت الانتظار
            retry_count += 1
            logger.info(f"⏳ انتظار بدء خادم WhatsApp... ({retry_count}/{max_retries})")
        
        raise Exception("فشل في بدء تشغيل خادم WhatsApp بعد عدة محاولات")
        
    except Exception as e:
        logger.error(f"❌ فشل تشغيل خادم WhatsApp: {str(e)}")
        raise

def cleanup(node_process):
    if node_process:
        try:
            # محاولة إغلاق جميع صفحات الواتساب أولاً
            try:
                response = requests.post('http://localhost:3003/stop-server', timeout=5)
                if response.status_code == 200:
                    logger.info("تم إيقاف خادم الواتساب")
            except Exception as e:
                logger.debug(f"لا يمكن الاتصال بخادم الواتساب: {str(e)}")

            # إيقاف عملية Node.js
            if platform.system() == 'Windows':
                subprocess.run(['taskkill', '/F', '/PID', str(node_process.pid)],
                             stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE)
            else:
                os.killpg(os.getpgid(node_process.pid), signal.SIGTERM)
            logger.info("✅ تم إيقاف خادم WhatsApp")
            time.sleep(2)  # انتظار قليلاً للتأكد من إغلاق الخادم
        except Exception as e:
            logger.error(f"❌ خطأ في إيقاف خادم WhatsApp: {str(e)}")

def restart_whatsapp_server():
    try:
        # محاولة إعادة تشغيل الخادم
        try:
            response = requests.post('http://localhost:3003/restart-server', timeout=5)
            if response.status_code == 200:
                logger.info("تم طلب إعادة تشغيل خادم الواتساب")
                time.sleep(3)  # انتظار إعادة التشغيل
                return start_node_server()
        except Exception as e:
            logger.debug(f"لا يمكن الاتصال بخادم الواتساب: {str(e)}")
            # إذا فشل الاتصال، نحاول إعادة التشغيل يدوياً
            return start_node_server()
    except Exception as e:
        logger.error(f"❌ خطأ في إعادة تشغيل خادم WhatsApp: {str(e)}")
        raise

def main():
    node_process = None
    try:
        app = create_app()
        node_process = start_node_server()

        logger.info("🚀 بدء تشغيل الخادم الرئيسي...")
        app.run(debug=False, host='0.0.0.0', port=5007)
    except KeyboardInterrupt:
        logger.info("\n🛑 جاري إيقاف الخوادم...")
    except Exception as e:
        logger.error(f"❌ خطأ في تشغيل الخادم الرئيسي: {str(e)}")
    finally:
        if node_process:
            cleanup(node_process)

if __name__ == '__main__':
    main() 