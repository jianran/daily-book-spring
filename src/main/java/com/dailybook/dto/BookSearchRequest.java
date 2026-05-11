package com.dailybook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookSearchRequest {
    private List<String> categories;
    private String apiKey;
    private Integer numberOfBooksPerCategory;
}
