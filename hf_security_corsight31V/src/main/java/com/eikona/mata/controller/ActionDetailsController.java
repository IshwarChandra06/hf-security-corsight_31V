package com.eikona.mata.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.eikona.mata.entity.ActionDetails;
import com.eikona.mata.repository.ActionDetailsRepository;
import com.eikona.mata.service.ActionDetailsService;

@Controller
public class ActionDetailsController {

	@Autowired
	private ActionDetailsService actionDetailsService;
	
	@Autowired
	private ActionDetailsRepository actionDetailsRepository;
	
	

	@GetMapping("/action-details")
	public String listActionDetails(Model model) {
		model.addAttribute("listActionDetails", actionDetailsService.getAll());
		return "actionDetails/actionDetails_list";
	}

	@GetMapping("/action-details/new")
	public String newActionDetails(Model model) {

		ActionDetails actionDetails = new ActionDetails();
		model.addAttribute("actionDatils", actionDetails);
		model.addAttribute("title", "New Exception");
		return "actionDetails/actionDetails_new";
	}

	@PostMapping("/action-details/add")
	public String saveActionDetails(@ModelAttribute("actionDetails") ActionDetails actionDetails, @Valid ActionDetails enitiy, Errors errors, String title,
			Model model) {

		if (errors.hasErrors()) {
			model.addAttribute("title", title);
			return "actionDetails/actionDetails_new";
		} else {
			model.addAttribute("message", "Add Successfully");
			actionDetailsService.save(actionDetails);
			return "redirect:/action-details";
		}
	}

	@GetMapping("/action-details/edit/{id}")
	public String editActionDetails(@PathVariable(value = "id") long id, Model model) {
		ActionDetails actionDetails = actionDetailsService.getById(id);
		model.addAttribute("actionDatils", actionDetails);
		model.addAttribute("title", "Update Exception");
		return "actionDetails/actionDetails_new";
	}

	@GetMapping("/action-details/delete/{id}")
	public String deleteActionDetails(@PathVariable(value = "id") long id) {
		this.actionDetailsService.deleteById(id);
		return "redirect:/action-details";
	}

	@GetMapping(value = "/api/action-details")
	public @ResponseBody DataTablesOutput<ActionDetails> dataTable(@Valid DataTablesInput input) {
		return (DataTablesOutput<ActionDetails>) actionDetailsService.getAllFrom(input);
	}
	
	@GetMapping("/action-datails/per-id/{id}")
	public String actionDetailsPerEachIdViewPage(@PathVariable(value = "id") long id, Model model) {
		model.addAttribute("id", id);
		return "actionDetails/actionDetails_perid";
	}
	
	@GetMapping(value = "/api/action-details/per-id")
	public @ResponseBody  List<ActionDetails> actionDetailsDatatable(@RequestParam Long id) {
		
		List<ActionDetails> actionDetailsList = actionDetailsRepository.findByActionIdCustom(id);
		return actionDetailsList;
	}
	

}
