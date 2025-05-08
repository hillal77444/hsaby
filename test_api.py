import requests
import json
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

# عنوان السيرفر
BASE_URL = "http://212.224.88.122:5007"

# إعداد إعادة المحاولة
retry_strategy = Retry(
    total=3,  # عدد المحاولات
    backoff_factor=1,  # وقت الانتظار بين المحاولات
    status_forcelist=[500, 502, 503, 504]  # الأخطاء التي تستدعي إعادة المحاولة
)
adapter = HTTPAdapter(max_retries=retry_strategy)
session = requests.Session()
session.mount("http://", adapter)
session.mount("https://", adapter)

def test_register():
    # بيانات التسجيل
    register_data = {
        "username": "test_user_3",
        "phone": "+966500000003",
        "password": "Test@123"
    }
    
    try:
        # إرسال طلب التسجيل
        response = session.post(
            f"{BASE_URL}/api/register",
            json=register_data,
            headers={"Content-Type": "application/json"},
            timeout=10  # مهلة 10 ثواني
        )
        
        print("\n=== اختبار التسجيل ===")
        print("Register Response:", response.status_code)
        print("Register Response Body:", response.text)
        
        return response.json() if response.ok else None
    except requests.exceptions.RequestException as e:
        print(f"Error during registration: {str(e)}")
        return None

def test_login():
    # بيانات تسجيل الدخول
    login_data = {
        "phone": "+966500000003",
        "password": "Test@123"
    }
    
    try:
        # إرسال طلب تسجيل الدخول
        response = session.post(
            f"{BASE_URL}/api/login",
            json=login_data,
            headers={"Content-Type": "application/json"},
            timeout=10  # مهلة 10 ثواني
        )
        
        print("\n=== اختبار تسجيل الدخول ===")
        print("Login Response:", response.status_code)
        print("Login Response Body:", response.text)
        
        return response.json() if response.ok else None
    except requests.exceptions.RequestException as e:
        print(f"Error during login: {str(e)}")
        return None

if __name__ == "__main__":
    print("بدء الاختبار...")
    register_result = test_register()
    
    if register_result:
        print("\nتم التسجيل بنجاح، جاري اختبار تسجيل الدخول...")
        login_result = test_login()
    else:
        print("\nفشل التسجيل، لن يتم اختبار تسجيل الدخول") 