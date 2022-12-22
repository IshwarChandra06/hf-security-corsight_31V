package com.eikona.mata.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.eikona.mata.entity.Action;
import com.eikona.mata.service.ActionService;
import com.eikona.mata.service.EmployeeService;

@Controller
public class ActionController {

	@Autowired
	private ActionService actionService;
	
	@Autowired
	private EmployeeService employeeService;
	

	@GetMapping("/action")
	public String actionList(Model model) {
		model.addAttribute("listAction", actionService.getAll());
		return "action/action_list";
	}
	
	@GetMapping("/play")
	public String playVideo(Model model) {
		return "play_video";
		
	}

	@GetMapping("/action/new")
	public String newAction(Model model) {
		model.addAttribute("listEmployee", employeeService.getAll());
		model.addAttribute("listAction", actionService.getAll());
		Action action = new Action();
		model.addAttribute("actionObj", action);
		model.addAttribute("title", "New Audit");
		return "action/action_new";
	}

	@PostMapping("/action/add")
	public String saveAction(@ModelAttribute("actionObj") Action actionObj, @Valid Action enitiy, Errors errors, String title,
			Model model) {

		if (errors.hasErrors()) {
			model.addAttribute("title", title);
			return "action/action_new";
		} else {
			model.addAttribute("message", "Add Successfully");
			actionService.save(actionObj);
			return "redirect:/action";
		}
	}

	@GetMapping("/action/edit/{id}")
	public String editAction(@PathVariable(value = "id") long id, Model model) {
		model.addAttribute("listEmployee", employeeService.getAll());
		Action action = actionService.getById(id);
		model.addAttribute("actionObj", action);
		model.addAttribute("title", "Update Audit");
		return "action/action_new";
	}

	@GetMapping("/action/delete/{id}")
	public String deleteAction(@PathVariable(value = "id") long id) {
		this.actionService.deleteById(id);
		return "redirect:/action";
	}

	@GetMapping(value = "/api/action")
	public @ResponseBody DataTablesOutput<Action> actionDatatable(@Valid DataTablesInput input) {
		return (DataTablesOutput<Action>) actionService.getAllFrom(input);
	}
	

	
}
