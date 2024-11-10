package com.naver.cafe.deploy

//<link rel="stylesheet" href="styles.css">
internal val htmlHeader = """
<!DOCTYPE html>
<html>
<head>
  <title>APK</title>
<style>
/* 기본 스타일 (모바일 우선) */
ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

li {
  display: flex;
  flex-direction: column;
  padding: 0px 0px;
}

li li {
  display: flex;
  flex-direction: column;
  padding: 0px 0px;
}

/* 들여쓰기 스타일 (모바일) */
ul ul { /* 하위 목록 들여쓰기 */
  padding-left: 5vw;
}

ul ul ul { /* 3단계 하위 목록 들여쓰기 */
  padding-left: 10vw;
}

/* 아이콘 스타일 (모바일) */
li a::before {
  content: '';
  display: inline-block;
  width: 3vw;
  height: 3vw;
  margin-right: 3vw;
}

/* 폴더 아이콘 */
li a::before {
  content: '📁'; /* 📁 파일 아이콘 */
}

li a[href${'$'}='.jpeg']::before,
li a[href${'$'}='.jpg']::before,
li a[href${'$'}='.png']::before {
  content: '🎨'; /* 🎨 파일 아이콘 */
}

/* 파일 아이콘 (Markdown, 텍스트) */
li a[href${'$'}='.md']::before,
li a[href${'$'}='.mapping']::before,
li a[href${'$'}='.pdf']::before,
li a[href${'$'}='.txt']::before {
  content: '📄'; /* 📄 파일 아이콘 */
}

/* 파일 아이콘 (JAR 파일) */
li a[href${'$'}='.jar']::before {
  content: '☕'; /* ☕ 파일 아이콘 */
}

/* 파일 아이콘 (APK 파일) */
li a[href${'$'}='.apk']::before {
  content: '📱'; /* 📱 파일 아이콘 */
}

/* 파일 아이콘 (AAB 파일) */
li a[href${'$'}='.aab']::before {
  content: '📦'; /* 📦 파일 아이콘 */
}

/* 링크 스타일 (모바일) */
li a {
  text-decoration: none;
  color: #333;
  font-size: 5vw;
  padding: 2vw 2vw;
  flex: 1;
}

li a:hover {
  background-color: #f0f0f0;
  border: 0px solid #ccc;
  padding: 2vw 2vw;
}

/* --- PC 스타일 --- */
@media screen and (min-width: 1000px) { /* PC 환경을 위한 스타일 */

  /* 리스트 들여쓰기 스타일 (PC) */
  ul ul {
    padding-left: 2rem;
  }

  ul ul ul {
    padding-left: 4rem;
  }

  /* 아이콘 스타일 (PC) */
  li a::before {
    width: 1.5rem; /* PC용 아이콘 크기 */
    height: 1.5rem;
    margin-right: 1rem;
  }

  /* 링크 스타일 (PC) */
  li a {
    font-size: 1.25rem; /* PC용 폰트 크기 */
    padding: 1rem;
  }

  li a:hover {
    background-color: #f0f0f0;
    border: 0;
    padding: 1rem;
  }
}

/* --- 다크모드 스타일 --- */
@media (prefers-color-scheme: dark) {
  body {
    background-color: #121212;
    color: #e0e0e0;
  }

  ul {
    background-color: #1e1e1e;
  }

  li {
    background-color: #1e1e1e;
    border-top: 1px solid #333;
  }

  li a {
    color: #e0e0e0; /* 다크모드에서는 글씨 색을 밝게 */
  }

  li a:hover {
    background-color: #333; /* 다크모드에서 호버시 어두운 배경 */
  }

  li a::before {
    color: #e0e0e0; /* 아이콘의 색상도 조정 가능 */
  }
}
</style>
</head>
<body>
""".trimIndent()

internal val htmlTail = """
</body>
</html>
""".trimIndent()
