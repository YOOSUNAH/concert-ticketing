package com.concertticketing.domain.user.entity;

public class User {

    private Long id;
    private String email;
    private String password;
    private String name;
    private int point;

    protected User() {
    }

    public User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
    }

    /**
     * 포인트 사용 (결제 시 차감)
     * - 음수 금액 차단
     * - 잔액 부족 시 예외
     */
    public void usePoint(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("사용할 포인트는 0 이상이어야 합니다.");
        }
        if (this.point < amount) {
            throw new IllegalStateException("포인트 잔액이 부족합니다.");
        }
        this.point -= amount;
    }

    /**
     * 포인트 환원 (환불 시 사용했던 포인트 되돌리기)
     * - 음수 금액 차단
     */
    public void refundPoint(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("환원할 포인트는 0 이상이어야 합니다.");
        }
        this.point += amount;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public int getPoint() {
        return point;
    }
}
