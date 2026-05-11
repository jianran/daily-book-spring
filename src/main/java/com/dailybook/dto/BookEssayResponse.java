package com.dailybook.dto;

import com.dailybook.model.Essay;

public class BookEssayResponse {
    private String status;
    private Essay essay;
    private String message;

    public BookEssayResponse() {}

    public BookEssayResponse(String status, Essay essay, String message) {
        this.status = status;
        this.essay = essay;
        this.message = message;
    }

    public String getStatus() { return status; }
    public Essay getEssay() { return essay; }
    public String getMessage() { return message; }

    public void setStatus(String status) { this.status = status; }
    public void setEssay(Essay essay) { this.essay = essay; }
    public void setMessage(String message) { this.message = message; }
}
