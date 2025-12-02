package taxi.tago.dto.Admin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import taxi.tago.constant.UserRole;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResponse {
    private boolean success;
    private String message;
    private String email;
    private UserRole role; // ADMIN 또는 USER
}