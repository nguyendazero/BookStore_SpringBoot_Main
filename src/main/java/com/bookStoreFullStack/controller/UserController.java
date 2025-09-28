package com.bookStoreFullStack.controller;

import java.sql.Date;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bookStoreFullStack.entity.Cart;
import com.bookStoreFullStack.entity.User;
import com.bookStoreFullStack.service.CartService;
import com.bookStoreFullStack.service.UserService;
import com.bookStoreFullStack.utils.MaHoa;

import jakarta.servlet.http.HttpSession;


@Controller
public class UserController {
	
	@Autowired
	private UserService userService;
	@Autowired
	private HttpSession session;
	@Autowired
	private JavaMailSender javaMailSender;
	@Autowired
	private CartService cartService;
	
	@GetMapping("/user/login-page")
	public String loginPage() {
		return "login";
	}
	
	@PostMapping("user/login")
	public String checkLogin(@RequestParam("username") String username, @RequestParam("password") String password, Model model) {
	    User user = userService.getUserByUsername(username);
	    
	    if (user != null) {
	        String salt = user.getSalt();
	        
	        String hashedPassword = MaHoa.toSHA256(password, salt);
	        
	        if (hashedPassword.equals(user.getPassword())) {
	            if (user.getRole() != 1) {
	                session.setAttribute("userLogin", user);
	                return "redirect:/home";
	            } else {
	                session.setAttribute("userLogin", user);
	                return "redirect:/home-admin";
	            }
	        } else {
	            model.addAttribute("error", "Tên người dùng hoặc mật khẩu không đúng!");
	            return "login";
	        }
	    } else {
	        model.addAttribute("error", "Tên người dùng hoặc mật khẩu không đúng!");
	        return "login";
	    }
	}

	
	@GetMapping("/user/logout")
	public String logout() {
		session.removeAttribute("userLogin");
		return "redirect:/home";
	}
	
	@GetMapping("/user/register-page")
	public String registerPage() {
		return "register";
	}
	
	@PostMapping("/user/register")
	public String register(
	        @RequestParam("username") String username,
	        @RequestParam("repassword") String repassword,
	        @RequestParam("password") String password,
	        @RequestParam("address") String address,
	        @RequestParam("date_of_birth") Date dateOfBirth,
	        @RequestParam("gender") String gender,
	        @RequestParam("email") String email,
	        @RequestParam("full_name") String fullName,
	        @RequestParam("telephone") String telephone,
	        Model model) {

	    if (!password.equals(repassword)) {
	        model.addAttribute("error", "Mật khẩu nhập lại không khớp!");
	        return "register";
	    }
	    
	    User userExist = userService.getUserByUsername(username);
	    User userEmail = userService.getUserByEmail(email);

	    if (userExist != null || userEmail != null) {
	        String error = (userExist != null) ? "Tên đăng nhập đã tồn tại!" : "Email này đã đăng ký tài khoản, hãy chọn email khác!";
	        model.addAttribute("error", error);
	        return "register";
	    }

	    String verificationCode = String.format("%06d", new Random().nextInt(999999));
	    LocalDateTime sentTime = LocalDateTime.now();

	    try {
	        SimpleMailMessage message = new SimpleMailMessage();
	        message.setTo(email);
            message.setFrom("gillkaijame@gmail.com");
	        message.setSubject("Xác thực tài khoản");
	        message.setText("Mã xác thực của bạn là: " + verificationCode + "\nMã xác thực có hiệu lực trong 1 phút.");
            
            javaMailSender.send(message);
	    } catch (MailException e) {
	        model.addAttribute("error", "Gửi email thất bại. Vui lòng thử lại!");
	        return "register";
	    }

	 // Lưu vào session
        session.setAttribute("username", username);
        session.setAttribute("password", password);
        session.setAttribute("address", address);
        session.setAttribute("dateOfBirth", dateOfBirth);
        session.setAttribute("gender", gender);
        session.setAttribute("email", email);
        session.setAttribute("fullName", fullName);
        session.setAttribute("telephone", telephone);
        session.setAttribute("verificationCode", verificationCode);
        session.setAttribute("sentTime", sentTime);

	    return "verify";
	}

	@PostMapping("/user/verify")
	public String verifyCode(
	        @RequestParam("username") String username,
	        @RequestParam("fullName") String fullName,
	        @RequestParam("dateOfBirth") Date dateOfBirth, 
	        @RequestParam("password") String password,
	        @RequestParam("address") String address,
	        @RequestParam("telephone") String telephone,
	        @RequestParam("email") String email,
	        @RequestParam("gender") String gender,
	        @RequestParam("verificationCode") String verificationCode,
	        @RequestParam("code") String code,
	        @RequestParam("sentTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime sentTime,
	        Model model) {

	    // Kiểm tra thời gian hết hạn
	    if (Duration.between(sentTime, LocalDateTime.now()).getSeconds() > 60) {
	        model.addAttribute("error", "Mã xác thực đã hết hạn. Vui lòng yêu cầu mã mới.");
	        return "verify";
	    }

	    if (!verificationCode.equals(code)) {
	        model.addAttribute("error", "Mã xác thực không chính xác.");
	        return "verify";
	    }

	    String salt = MaHoa.generateSalt();
	    String hashedPassword = MaHoa.toSHA256(password, salt);
	    
	    User user = new User();
	    user.setUserName(username);
	    user.setPassword(hashedPassword); 
	    user.setSalt(salt); 
	    user.setFullName(fullName);
	    user.setAddress(address);
	    user.setTelephone(telephone);
	    user.setEmail(email);
	    user.setDateOfBirth(dateOfBirth);
	    user.setGender(gender);
	    user.setRole(0);
	    
	    userService.saveUser(user);

	    // Tạo giỏ hàng cho người dùng mới
	    Cart cart = new Cart();
	    cart.setUser(user);
	    cartService.saveCart(cart);

	    session.invalidate();
	    model.addAttribute("error", "Đăng ký thành công, hãy đăng nhập!");
	    return "login";
	}


	
	@GetMapping("/user/change-pass-form")
	public String changePassPage(Model model) {
		return "change-pass";
	}
	
	@PostMapping("/user/change-password")
	public String ChangePassword(
	        @RequestParam("currentPassword") String currentPassword,
	        @RequestParam("newPassword") String newPassword,
	        @RequestParam("confirmPassword") String confirmPassword,
	        Model model) {
	    
	    String error = "";
	    User userLogin = (User) session.getAttribute("userLogin");
	    
	    if (userLogin == null) {
	        return "redirect:/user/login-page";
	    }
	    
	    // Mã hóa mật khẩu hiện tại nhập vào với salt của người dùng
	    String hashedCurrentPassword = MaHoa.toSHA256(currentPassword, userLogin.getSalt());
	    
	    // Kiểm tra mật khẩu cũ có chính xác không
	    if (!hashedCurrentPassword.equals(userLogin.getPassword())) {
	        error += "Mật khẩu cũ không chính xác!";
	    } else {
	        if (!newPassword.equals(confirmPassword)) {
	            error += "Mật khẩu nhập lại không khớp!";
	        } else {
	            if (newPassword.equals(currentPassword)) {
	                error += "Mật khẩu mới không được trùng mật khẩu cũ!";
	            } else {
	                // Mã hóa mật khẩu mới với salt hiện tại
	                String hashedNewPassword = MaHoa.toSHA256(newPassword, userLogin.getSalt());
	                
	                userLogin.setPassword(hashedNewPassword);
	                userService.saveUser(userLogin);
	                
	                error = "Đổi mật khẩu thành công!";
	                model.addAttribute("message", error);
	                return "success";
	            }
	        }
	    }
	    
	    model.addAttribute("error", error);
	    return "change-pass";
	}

	
	@GetMapping("/user/update-info-form")
	public String updateInforForm(Model model) {
		User userLogin = (User) session.getAttribute("userLogin");
		if(userLogin == null) {
			return "redirect:/user/login-page";
		}
		model.addAttribute("user", userLogin);
		return "change-infor";
	}
	
	@PostMapping("user/update")
	public String updateUser(@RequestParam("fullName") String fullName,
							 @RequestParam("gender") String gender,
							 @RequestParam("telephone") String telephone,
							 @RequestParam("address") String address,
							 @RequestParam("dateOfBirth") Date dateOfBirth,
							 @RequestParam("email") String email, Model model) {
		User userLogin = (User) session.getAttribute("userLogin");
		if(userLogin == null) {
			return "redirect:/user/login-page";
		}
		
		userLogin.setFullName(fullName);
		userLogin.setGender(gender);
		userLogin.setTelephone(telephone);
		userLogin.setAddress(address);
		userLogin.setEmail(email);
		userLogin.setDateOfBirth(dateOfBirth);
		
		userService.saveUser(userLogin);
		model.addAttribute("error", "Thay đổi thông tin thành công!");
		model.addAttribute("user", userLogin);
		return "success";
	}
	
	/*****************************ADMIN*********************************/
	
	@GetMapping("/admin/user")
	public String ManagerUser(Model model) {
		List<User> users = userService.getAllUsers();
		model.addAttribute("users", users);
		return "admin/user";
	}
	
	@GetMapping("/admin/users/edit-role/{id}")
	public String editRole(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
		User userLogin = (User) session.getAttribute("userLogin");
		if(userLogin == null) {
			return "redirect:/user/login-page";
		}
		
		if(userLogin.getUserName().equals("admin")) {
			User userGet = userService.getUserById(id);
			if(userGet.getRole() == 0) {
				userGet.setRole(1);
				userService.saveUser(userGet);
			}else {
				userGet.setRole(0);
				userService.saveUser(userGet);
			}
		}else {
			redirectAttributes.addFlashAttribute("error", "Bạn không có quyền này!");
		}
		return "redirect:/admin/user";
	}
	
	@GetMapping("/admin/users/delete/{id}")
	public String deleteUser(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
		User userLogin = (User) session.getAttribute("userLogin");
		if(userLogin == null) {
			return "redirect:/user/login-page";
		}
		
		if(userLogin.getUserName().equals("admin")) {
			userService.deleteUser(id);
		}else {
			redirectAttributes.addFlashAttribute("error", "Bạn không có quyền này!");
		}
		return "redirect:/admin/user";
	}
	

}
