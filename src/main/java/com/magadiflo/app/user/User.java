package com.magadiflo.app.user;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
}
