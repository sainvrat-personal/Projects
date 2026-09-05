package com.swifteats.common.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class ApiRootController {

    @GetMapping("/")
    public RedirectView root() {
        return new RedirectView("/swagger-ui.html");
    }
}
