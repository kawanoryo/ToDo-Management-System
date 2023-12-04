package com.dmm.task;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.dmm.task.data.entity.Tasks;
import com.dmm.task.data.repository.TaskRepository;
import com.dmm.task.form.TaskForm;
import com.dmm.task.service.AccountUserDetails;

@Controller

public class TaskController {
	@GetMapping("/loginForm")
	String loginForm() {
		return "login";
	}

	@GetMapping("/main/create/{date}")
	public String create(Model model, @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
		return "create";
	}

	@Autowired
	private TaskRepository repo;

	@PostMapping("/main/create")
	public String taskCreate(Model model, TaskForm form, @AuthenticationPrincipal AccountUserDetails user) {

		Tasks task = new Tasks();
		task.setName(user.getName());
		task.setTitle(form.getTitle());
		task.setText(form.getText());
		task.setDate(form.getDate().atTime(0, 0));

		repo.save(task);

		return "redirect:/main";
	}

	@GetMapping("/main/edit/{id}")
	public String edittask(@PathVariable Integer id, Model model) {
		Tasks task = repo.findById(id).orElse(null);

		if (task != null) {
			model.addAttribute("task", task);
			return "edit";
		} else {
			return "redirect:/main";
		}
	}

	@PostMapping("/main/edit/{id}")
	public String saveEditedTask(Model model, TaskForm form, @PathVariable Integer id,
			@AuthenticationPrincipal AccountUserDetails user) {

		Tasks task = repo.findById(form.getId()).orElse(null);

		if (task != null) {
			task.setTitle(form.getTitle());
			task.setText(form.getText());
			task.setDate(form.getDate().atTime(0, 0));
			task.setDone(form.isDone());
			repo.save(task);
		}
		return "redirect:/main";
	}

	@PostMapping("/main/delete/{id}")
	public String deleteTask(@PathVariable Integer id, @AuthenticationPrincipal AccountUserDetails user) {
		Tasks task = repo.findById(id).orElse(null);

		if (task != null) {

			repo.delete(task);
		}

		return "redirect:/main";
	}

	@GetMapping("/main")
	public String main(Model model, @AuthenticationPrincipal AccountUserDetails user,
			@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

		List<List<LocalDate>> month = new ArrayList<>();

		List<LocalDate> week = new ArrayList<>();

		LocalDate day, start, end;
		if (date == null) {

			day = LocalDate.now();
			day = LocalDate.of(day.getYear(), day.getMonthValue(), 1);
		} else {
			day = date;
		}

		model.addAttribute("month", day.format(DateTimeFormatter.ofPattern("yyyy年MM月")));

		model.addAttribute("prev", day.minusMonths(1));
		model.addAttribute("next", day.plusMonths(1));

		DayOfWeek w = day.getDayOfWeek();
		day = day.minusDays(w.getValue());
		start = day;

		for (int i = 1; i <= 7; i++) {
			week.add(day);
			day = day.plusDays(1);

		}

		month.add(week);

		week = new ArrayList<>();

		int leftOfMonth = day.lengthOfMonth() - day.getDayOfMonth();
		leftOfMonth = day.lengthOfMonth() - leftOfMonth;
		leftOfMonth = 7 - leftOfMonth;

		for (int i = 7; i <= day.lengthOfMonth() + leftOfMonth; i++) {

			week.add(day);

			w = day.getDayOfWeek();

			if (w == DayOfWeek.SATURDAY) {
				month.add(week);
				week = new ArrayList<>();
			}

			day = day.plusDays(1);

		}

		DayOfWeek endofmonth = day.getDayOfWeek();
		int next = 7 - endofmonth.getValue();
		if (next == 0) {
			next = 7;
		}

		for (int n = 1; n <= next; n++) {
			week.add(day);
			day = day.plusDays(1);

		}
		month.add(week);

		end = day;
		MultiValueMap<LocalDate, Tasks> tasks = new LinkedMultiValueMap<LocalDate, Tasks>();

		List<Tasks> list;

		if (user.getUsername().equals("admin")) {

			list = repo.findAllByDateBetween(start.atTime(0, 0), end.atTime(0, 0));

		} else {

			list = repo.findByDateBetween(start.atTime(0, 0), end.atTime(0, 0), user.getName());
		}

		for (Tasks task : list) {
			tasks.add(task.getDate().toLocalDate(), task);
		}

		model.addAttribute("matrix", month);

		model.addAttribute("tasks", tasks);

		return "main";
	}

}
