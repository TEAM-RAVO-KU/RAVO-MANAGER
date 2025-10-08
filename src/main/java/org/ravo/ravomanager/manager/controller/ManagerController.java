package org.ravo.ravomanager.manager.controller;

import org.ravo.ravomanager.manager.service.ManagerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/manager")
public class ManagerController {

    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @GetMapping("/status")
    public String status(Model model) {
        model.addAllAttributes(managerService.getAllStatuses());
        return "manager/status";
    }
}