import os
import re
import subprocess
import sys
import time
from urllib.parse import quote_plus


def find_project_root():
    """
    주어진 디렉토리부터 상위 디렉토리를 순차적으로 검색하여 settings.gradle.kts 파일의 경로를 반환합니다.

    Args:
      start_dir: 검색을 시작할 디렉토리

    Returns:
      str: settings.gradle.kts 파일의 절대 경로 또는 None (파일을 찾지 못한 경우)
    """

    current_dir = os.path.dirname(os.path.abspath(__file__))
    while True:
        settings_file = os.path.join(current_dir, "settings.gradle.kts")
        if os.path.exists(settings_file):
            return settings_file

            # 상위 디렉토리로 이동
        parent_dir = os.path.dirname(current_dir)
        if parent_dir == current_dir:  # 루트 디렉토리에 도달한 경우
            return None
        return parent_dir


def cut_by_bytes_internal(string, max_bytes=200):
    """문자열을 바이트 단위로 잘라내는 함수

    Args:
      string: 자를 문자열
      max_bytes: 최대 바이트 수

    Returns:
      잘린 문자열
    """
    encoded_string = string.encode('utf-8')  # UTF-8 인코딩으로 변환
    if len(encoded_string) > max_bytes:
        return encoded_string[:max_bytes].decode('utf-8', 'ignore')
    else:
        return string


def get_clipboard_data():
    """ 클립보드에서 데이터를 읽어오는 함수 """
    return subprocess.check_output("pbpaste", universal_newlines=True)


def set_clipboard_data(data):
    """ 데이터를 클립보드에 복사하는 함수 """
    process = subprocess.Popen(['pbcopy'], stdin=subprocess.PIPE)
    process.communicate(data.encode('utf-8'))


def url_encode():
    print(quote_plus(sys.argv[0]))


def cut_by_bytes():
    if len(sys.argv) == 1:
        arg1 = sys.argv[0]
        arg2 = 200
    elif len(sys.argv) == 2:
        arg1 = sys.argv[0]
        arg2 = int(sys.argv[1])
    else:
        print("cut_by_bytes() 잘못된 인자 개수입니다.")
        exit(1)

    string = arg1
    max_bytes = arg2

    print(cut_by_bytes_internal(string, max_bytes))


# project root 찾기 : 상위 폴더중 settings.gradle.kts 파일이 있는 폴더
project_root = find_project_root()


def creator_branch_name():
    # 클립보드에서 데이터를 읽어옵니다.
    clip_data = get_clipboard_data().strip()
    ticket_no = find_ticket_no(clip_data)
    message = after_text(ticket_no, clip_data)

    # 메시지 처리 (이미지 제거, 불필요한 단어 삭제 등)
    message = message.replace(" ", "_")
    message = re.sub(r"[^\w\s>,]", '', message)
    message = re.sub(r'feature|Android|android|고객문의|안드로이드', '', message)
    message = re.sub(r"_+", "_", message)
    message = re.sub(r"_*>_*", ">", message)
    message = re.sub(r"_*,_*", ",", message)
    message = re.sub(r'^[ _-]+', '', message)
    message = re.sub("[ _-]+$", '', message)

    # 브랜치 이름 생성
    branch_name = f"feature/{ticket_no}-{message}"

    # 브랜치 이름을 최대 250바이트로 자르기
    branch_name = cut_by_bytes_internal(branch_name)

    # 브랜치 이름을 클립보드에 복사
    set_clipboard_data(branch_name)
    print(branch_name)
    return branch_name


def after_text(find_text, text):
    index = text.find(find_text)
    if index != -1:
        message = text[index + len(find_text):].strip()
    else:
        message = text.strip()
    return message


def find_ticket_no(message):
    # ticket 번호 추출
    matches = re.findall(r"(TICKET-[0-9]+)", message)
    if matches:
        ticket_no = matches[-1]
    else:
        ticket_no = "NO-TICKET"
    return ticket_no


func_name = sys.argv[1]
sys.argv = sys.argv[2:]
eval(func_name)()
