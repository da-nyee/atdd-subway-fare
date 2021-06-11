package wooteco.subway.member.domain;

public abstract class User {
    private Long id;
    private String email;
    private Integer age;

    public User() {
    }

    protected User(Long id, String email, Integer age) {
        this.id = id;
        this.email = email;
        this.age = age;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Integer getAge() {
        return age;
    }

    public abstract boolean isLoginMember();
}
