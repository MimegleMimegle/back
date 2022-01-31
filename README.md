# 밈글밈글(MemegleMemegle) - BackEnd

<img src="https://user-images.githubusercontent.com/70641418/151412036-345d6b9d-2657-459d-920a-def5be916f1c.jpg">

</br>

## 🧧 서비스 소개
인터넷에서 유명하는 합성 소스 또는 유행을 의미하는 밈과  
새롭게 생겨나는 신조어를 사전 형식으로 검색 할 수 있는 사이트입니다.
</br>

<a href="https://memegle.xyz/">프로젝트  사이트로 이동</a>

<a href="https://enormous-duck-a5d.notion.site/8-5510f20898534129bd1b728b92d92870">프로젝트 노션으로 이동</a>

</br>

## 📆 프로젝트 기간
2021.12.18. (토) ~ 2021.01.28. (금)

</br>

## 👥 팀원

- Back-End    

  
<code><a href="https://github.com/Zabee52">김용빈(팀장)</a></code>  
  
<code><a href="https://github.com/yarogono">임전혁</a></code>
  
</br>
    
- Front-End

<code><a href="https://github.com/undriedspring">이한샘</a></code>  
  
<code><a href="https://github.com/zubetcha">정주혜</a></code>
  
<code><a href="https://github.com/zhiyeonyi">이지연</a></code>

</br>

## 스택

<img src="https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white">  <img src="https://img.shields.io/badge/SpringBoot-6DB33F?style=for-the-badge&logo=Spring Boot&logoColor=white">  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=MySQL&logoColor=white">  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=Redis&logoColor=white"> </br>
<img src="https://img.shields.io/badge/Spring Data JPA-6DB33F?style=for-the-badge">
<img src="https://img.shields.io/badge/QueryDSL-3874D8?style=for-the-badge">
<img src="https://img.shields.io/badge/Youtube Data API v3-FF0000?style=for-the-badge&logo=YouTube&logoColor=white">


## 아키텍처
  
<img src="https://user-images.githubusercontent.com/70641418/151467454-da82b310-6249-4480-9204-8a4ace733ba6.JPG">
 
</br>
</br>

## DB설계

<img src="https://user-images.githubusercontent.com/70641418/151432549-bf519850-4146-471f-8cee-5e51bb932c88.png">

</br>  
  
## ✒Trouble Shooting


<details>
    <summary>
        QueryDSL의 랜덤 목록 불러오기 NumberExpression.random().asc() 기능이 MySQL에서는 작동하지 않는 문제
    </summary>
    <div markcown="1">
        - 엄밀히 따지면 문제는 아니다. 그냥 MySQL이 해당 랜덤 기능을 지원하지 않을 뿐이다.
- 지원 가능하도록 JPQLTemplates를 튜닝해주면 된다.
- 참고로 이 기능은 인덱싱이 통하지 않기 때문에 매우 무겁게 작동한다. 레코드가 많다면 인덱싱을 위한 편법을 사용해줘야 할 수도 있다.

```java
public class MySqlJpaTemplates extends JPQLTemplates{

    public static finalMySqlJpaTemplatesDEFAULT = new MySqlJpaTemplates();

    public MySqlJpaTemplates() {
        this(DEFAULT_ESCAPE);
        add(Ops.MathOps.RANDOM, "rand()");
        add(Ops.MathOps.RANDOM2, "rand({0})");
    }

    public MySqlJpaTemplates(charescape) {
        super(escape);
    }
}
```

적용예

```java
private List<Quiz> randomQuizPick(int count) {
        // count 만큼의 레코드를 랜덤하게 받아오는 구문
				// MySqlJpaTemplates.DEFAULT : NumberExpression.random().asc()를 MySQL에서 사용 가능하도록
				// 튜닝한 템플릿.
        JPAQuery<Quiz> query = new JPAQuery<>(entityManager, MySqlJpaTemplates.DEFAULT);
        QQuiz qQuiz = new QQuiz("quiz");

        List<Quiz> quizList = query.from(qQuiz)
                .orderBy(NumberExpression.random().asc())
                .limit(count)
                .fetch();

        return quizList;
    }
```   
        
        
  </div>
</details>


<details>
    <summary>
        QueryDSL 사용시 Handler dispatch failed; nested exception is java.lang.NoSuchFieldError: TREATED_PATH 에러 발생
    </summary>
    <div markcown="1">
        - QueryDSL에 대한 버전을 명확하게 명세하지 않아 발생한 문제였다.
- Gradle의 의존성 부분에 `implementation "com.querydsl:querydsl-core:${queryDslVersion}"` 추가하여 해결하였다. 변수 부분은 5.0.0으로 대체해도 좋다.

```java
buildscript {
    ext {
        queryDslVersion = "5.0.0"
    }
}

dependencies {
		// ...

    //querydsl 추가
    implementation "com.querydsl:querydsl-jpa:${queryDslVersion}"
    implementation "com.querydsl:querydsl-apt:${queryDslVersion}"
    implementation "com.querydsl:querydsl-core:${queryDslVersion}"
}
```    
        
  </div>
</details>

	    
<details>
    <summary>
        퀴즈 문제 조회시 문제가 중복으로 등장하는 에러 발생
    </summary>
    <div markcown="1">
        초보적인 문제였다. join을 수행하면 레코드의 중복이 발생하는데, 이를 간과하여 그대로 출력되는 문제였다.

distinct() 조건을 주어 해결하였다.

```java
private List<Quiz> randomQuizPick(String category) {
    // count 개수 만큼의 레코드를 랜덤하게 받아오는 구문
    // MySqlJpaTemplates.DEFAULT : NumberExpression.random().asc()를 MySQL 에서 사용 가능하도록 튜닝한 템플릿.
    JPAQuery<Quiz> query = new JPAQuery<>(entityManager, MySqlJpaTemplates.DEFAULT);
    QQuiz qQuiz = new QQuiz("quiz");
    QQuizBank qQuizBank = QQuizBank.quizBank;

    return query.from(qQuiz).distinct()
            .leftJoin(qQuiz.choiceList, qQuizBank)
            .fetchJoin()
            .where(qQuiz.category.eq(category))
            .orderBy(NumberExpression.random().asc())
            .fetch();
}
```
	     
        
  </div>
</details>
	    
	    
