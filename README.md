# 🎭 CurtainCall
***

<img src="/assets/logo.png">

## 💬 프로젝트 소개
***
> 커튼콜은 공연의 마지막을 장식하는 **'커튼콜'** 처럼 예매의 시작과 끝을 책임지는 서비스라는 의미를 담고있는 B2C 서비스 입니다.

## 🧩 ERD
***
<img src="/assets/erd.png">

## ⛓ 와이어 프레임
***
<img src="/assets/wire-frame.png">

## 📌 프로젝트 구조
***

### 백엔드
```
src
├── CurtaincallApplication.java
├── category
│   ├── controller
│   ├── domain
│   ├── dto
│   ├── repository
│   └── service
├── chat
│   ├── controller
│   ├── document
│   ├── dto
│   ├── repository
│   └── service
├── global
│   ├── config
│   ├── entity
│   └── exception
├── ...
```
### 프론트엔드
```
src
├── App.css
├── App.jsx
├── api
│   ├── categoryApi.js
│   ├── chatApi.js
│   ├── faqApi.js
│   ├── orderApi.js
│   ├── paymentApi.js
│   ├── productApi.js
│   ├── specialProductApi.js
│   └── userApi.js
├── assets
│   ├── favicon.png
│   ├── hot.png
│   └── img.png
├── components
│   ├── CancelBtn.jsx
│   ├── category
│   │   └── ...
│   └── ...
├── hooks
│   ├── UseToggleActive.jsx
│   └── UseUserRole.jsx
├── index.css
├── main.jsx
├── pages
│   ├── Home.jsx
│   ├── inquiry
│   │   └── ...
│   ├── ...
└── utils
    ├── endpoint.js
    ├── fetcher.js
    └── webSocket.js

```

## 🛠 기술 스택
***
### 프론트 엔드
<img src="https://img.shields.io/badge/react-61DAFB?style=for-the-badge&logo=react&logoColor=black">
<img src="https://img.shields.io/badge/javascript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black">
<img src="https://img.shields.io/badge/mui-007FFF?style=for-the-badge&logo=mui&logoColor=white">
<img src="https://img.shields.io/badge/html5-E34F26?style=for-the-badge&logo=html5&logoColor=white">
<img src="https://img.shields.io/badge/Css-663399?style=for-the-badge&logo=css&logoColor=white">

### 백엔드
<img src="https://img.shields.io/badge/spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white">
<img src="https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
<img src="https://img.shields.io/badge/springsecurity-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white">
<img src="https://img.shields.io/badge/websocket-007396?style=for-the-badge&logo=websocket&logoColor=white">

### 데이터베이스
<img src="https://img.shields.io/badge/mysql-4479A1?style=for-the-badge&logo=mysql&logoColor=white">
<img src="https://img.shields.io/badge/mongodb-47A248?style=for-the-badge&logo=mongodb&logoColor=white">
<img src="https://img.shields.io/badge/redis-FF4438?style=for-the-badge&logo=redis&logoColor=white">


### 메시징
<img src="https://img.shields.io/badge/apache%20kafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=white">

### 스토리지
<img src="https://img.shields.io/badge/amazons3-569A31?style=for-the-badge&logo=amazons3&logoColor=white">

### 배포 및 서버
<img src="https://img.shields.io/badge/GCP-4285F4?style=for-the-badge&logo=googlecloud&logoColor=white">
<img src="https://img.shields.io/badge/nginx-009639?style=for-the-badge&logo=nginx&logoColor=white">
<img src="https://img.shields.io/badge/docker-2496ED?style=for-the-badge&logo=docker&logoColor=white">

## 🔥 주요기능
***

### 1. 회원

|              회원가입                 |              권한 별 기능              |
|:---------------------------------:|:---------------------------------:|
| <img src="/assets/user-join.png"> | <img src="/assets/user-role.png"> |

* 회원가입
    * 이메일 형식의 아이디로 회원가입
    * 이메일, 비밀번호, 이름, 전화번호 유효성 검사
* 권한
    * 회원
        * 예매 및 문의 내역 조회
        * 개인정보 수정
    * 관리자
        * 회원 목록 조회 및 회원 활성화 변경
        * 카테고리, 상품, 문의 관리
* 로그인
    * 로그인 시 쿠키로 JWT 토큰 발행

### 2. 주문 및 결제

|               주문               |                결제                |
|:------------------------------:|:--------------------------------:|
| <img src="/assets/order.png"/> | <img src="/assets/payment.png">  |

* 주문
    * 해당 좌석이 이미 결제 진행 중인지 검사
    * 무한 대기 방지를 위해 결제 단계에서 30분 제한 설정(초과 시 좌석 홀딩 해제)
* 결제
    * 아이엠 포트 API를 사용하여 결제 및 결제 취소

### 3. 1:1 채팅 상담

|                권한-회원                |               권한-관리자                |          체팅 및 채팅 취소           |
|:-----------------------------------:|:-----------------------------------:|:-----------------------------:|
| <img src="/assets/chat-user.png" /> | <img src="/assets/chat-admin.png"/> | <img src="/assets/chat.png"/> |

* 웹소켓을 사용하여 실시간 문의 구현
* STOMP에서 기본으로 제공되는 메시지 브로커 대신 레디스의 PUB/SUB 사용
* 권한별 기능
    * 회원
        * 관리자에게 채팅 신청
        * 관리자가 입장 후 채팅 가능
        * 상담중인 채팅 종료
    * 관리자
        * 상담 중인 채팅방 기록 조회
        * 상담 대기중인 채팅방 조회
        * 상담중인 채팅 종료

### 4. FAQ

|            FAQ-전체             |                 FAQ-필터링                 |
|:-----------------------------:|:---------------------------------------:|
| <img src="/assets/faq.png" /> | <img src="/assets/faq-filtering.png" /> |

* 자주 묻는 질문 항목 별로 필터링 및 10개 단위로 페이징
* 권한별 기능
    * 사용자
        * 자주 묻는 질문 조회만 가능
    * 관리자
        * 조회 및 추가, 수정, 삭제 가능

### 5. 문의

|             동적쿼리-1(상태)              |            동적쿼리-2(상태+유형)            |          동적쿼리-3(상태+유형+제목)           |
|:-----------------------------------:|:-----------------------------------:|:-----------------------------------:|
| <img src="/assets/inquiry-1.png" /> | <img src="/assets/inquiry-2.png" /> | <img src="/assets/inquiry-3.png" /> |

* 상태, 유형, 제목에 따라 동적으로 필터링 가능
* 10개 단위로 페이징 가능

### 6. 추천

|             사용자 클릭 기반 추천             |               연관 상품 추천               |
|:------------------------------------:|:------------------------------------:|
| <img src="/assets/recommend-1.png"/> | <img src="/assets/recommend-2.png"/> |


* 카프카를 사용하여 로그를 수집 후 사용자에게 추천 상품 제공
    * 사용자 클릭 기반 추천
        * 사용자가 가장 많이 클릭한 작품의 장르 중 인기 상품 제공
    * 연관 상품 추천
        * 모든 사용자의 클릭 로그 저장 후 연쇄 클릭이 잦은 상품을 기반으로 제공

### 특가상품

|             특가상품              |
|:-----------------------------:|
| <img src="/assets/sale.png"/> |

* 관리자가 특가상품을 등록 시 레디스에 값을 저장하여 캐싱된 값을 조회