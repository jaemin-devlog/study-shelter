package likelion.simsim.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 루트 경로 접속 시 정적 index.html 로 자연스럽게 연결합니다.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }
}
