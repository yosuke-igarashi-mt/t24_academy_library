package jp.co.metateam.library.controller;

import java.util.List;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.validation.Valid;
import jp.co.metateam.library.model.Account;
import jp.co.metateam.library.model.RentalManage;
import jp.co.metateam.library.model.Stock;
import jp.co.metateam.library.model.BookMst;
import jp.co.metateam.library.service.AccountService;
import jp.co.metateam.library.model.RentalManageDto;
import jp.co.metateam.library.service.RentalManageService;
import jp.co.metateam.library.service.StockService;
import jp.co.metateam.library.service.BookMstService;
import jp.co.metateam.library.values.RentalStatus;
import jp.co.metateam.library.constants.Constants;
import org.springframework.validation.FieldError;

import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 貸出管理関連クラスß
 */
@Log4j2
@Controller
public class RentalManageController {

    private final BookMstService bookMstService;
    private final StockService stockService;
    private final AccountService accountService;
    private final RentalManageService rentalManageService;

    @Autowired
    public RentalManageController(
            AccountService accountService,
            RentalManageService rentalManageService,
            StockService stockService,
            BookMstService bookMstService) {
        this.accountService = accountService;
        this.rentalManageService = rentalManageService;
        this.stockService = stockService;
        this.bookMstService = bookMstService;
    }

    /**
     * 貸出一覧画面初期表示
     * 
     * @param model
     * @return
     */
    @GetMapping("/rental/index")
    public String index(Model model) {
        // 貸出管理テーブルから全件取得
        List<RentalManage> rentalManageList = this.rentalManageService.findAll();
        // 貸出一覧画面に渡すデータをmodelに追加
        model.addAttribute("rentalManageList", rentalManageList);

        // 貸出一覧画面に遷移
        return "rental/index";
    }

    @GetMapping("/rental/add")
    public String add(Model model, @ModelAttribute RentalManageDto rentalManageDto) {
        List<Stock> stockList = this.stockService.findStockAvailableAll();
        List<Account> accounts = this.accountService.findAll();

        model.addAttribute("rentalStatus", RentalStatus.values());
        model.addAttribute("stockList", stockList);
        model.addAttribute("accounts", accounts);

        if (!model.containsAttribute("rentalManageDto")) {
            model.addAttribute("rentalManageDto", new RentalManageDto());

        }

        return "rental/add";
    }

    @PostMapping("/rental/add")
    public String save(@Valid @ModelAttribute RentalManageDto rentalManageDto, BindingResult result,
            RedirectAttributes ra, Model model) {

        try {
            Integer status = rentalManageDto.getStatus();
            if (status == 0 || status == 1) {

                String stockId = rentalManageDto.getStockId();
                Long stockSum = this.rentalManageService.countByStatusAndStockId(stockId);

                if (!(stockSum == 0)) {
                    Date expectedRentalOn = rentalManageDto.getExpectedRentalOn();
                    Date expectedReturnOn = rentalManageDto.getExpectedReturnOn();
                    Long stockNum = this.rentalManageService.countByStockIdAndExpectedReturn(expectedRentalOn,
                            expectedReturnOn, stockId);

                    if (!(stockSum == stockNum)) {
                        String rentalError = "この期間は貸出できません";
                        result.addError(new FieldError("rentalManageDto", "expectedRentalOn", rentalError));
                        result.addError(new FieldError("rentalManageDto", "expectedReturnOn", rentalError));

                    }
                }
            }

            if (result.hasErrors()) {
                throw new Exception("Validation error.");
            }
            ;
            // 登録処理
            this.rentalManageService.save(rentalManageDto);

            return "redirect:/rental/index";
        } catch (Exception e) {
            log.error(e.getMessage());

            ra.addFlashAttribute("rentalManageDto", rentalManageDto);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.rentalManageDto", result);
            List<Stock> stockList = this.stockService.findStockAvailableAll();
            List<Account> accounts = this.accountService.findAll();

            model.addAttribute("rentalStatus", RentalStatus.values());
            model.addAttribute("stockList", stockList);
            model.addAttribute("accounts", accounts);
            return "rental/add";
        }
    }

    @GetMapping("/rental/{id}/edit")
    public String edit(@PathVariable("id") String id, Model model) {

        List<Account> accounts = this.accountService.findAll();
        List<Stock> stockList = this.stockService.findStockAvailableAll();

        model.addAttribute("accounts", accounts);
        model.addAttribute("stockList", stockList);
        model.addAttribute("rentalStatus", RentalStatus.values());

        if (!model.containsAttribute("rentalManageDto")) {
            // model.addAttribute("RentalManageDto", new RentalManageDto());

            // DBから編集する貸出管理番号のレコードを取得する
            RentalManage rentalManage = this.rentalManageService.findById(Long.valueOf(id));
            // レコードのデータを挿入する箱を作る
            RentalManageDto rentalManageDto = new RentalManageDto();
            // 箱にデータを移す

            rentalManageDto.setId(rentalManage.getId());
            rentalManageDto.setEmployeeId(rentalManage.getAccount().getEmployeeId());
            rentalManageDto.setExpectedRentalOn(rentalManage.getExpectedRentalOn());
            rentalManageDto.setExpectedReturnOn(rentalManage.getExpectedReturnOn());
            rentalManageDto.setStatus(rentalManage.getStatus());
            rentalManageDto.setStockId(rentalManage.getStock().getId());

            model.addAttribute("rentalManageDto", rentalManageDto);
        }

        return "rental/edit";

    }

    @PostMapping("/rental/{id}/edit")
    public String update(@PathVariable("id") String id, @Valid @ModelAttribute RentalManageDto rentalManageDto,
            BindingResult result, Model model, RedirectAttributes ra) {
        try {

            // 在庫ステータスのチェック
            // 変更前の貸出情報を取得
            RentalManage rentalManage = this.rentalManageService.findById(Long.valueOf(id));
            // 変更前と変更後の貸出ステータスを取得
            Optional<String> validErrorOptional = rentalManageDto.isStatusError(rentalManage.getStatus());
            // Optionalが空でない場合のみエラーを処理する
            validErrorOptional.ifPresent(validError -> {
                if (!validError.isEmpty()) {
                    result.addError(new FieldError("rentalManageDto", "status", validErrorOptional.get()));

                }

            });

            // 貸出ステータスのチェック(rentalmanageDTOでチェック→ここで呼び出す)
            Integer status = rentalManageDto.getStatus();
            if (status == 0 || status == 1) {

                String stockId = rentalManageDto.getStockId();
                Long stockSum = this.rentalManageService.countByStatusAndNotId(Long.parseLong(id), stockId);

                if (!(stockSum == 0)) {
                    Date expectedRentalOn = rentalManageDto.getExpectedRentalOn();
                    Date expectedReturnOn = rentalManageDto.getExpectedReturnOn();
                    Long stockNum = this.rentalManageService.countByStatusAndExpectedReturnBeforeAndNotId(
                            expectedRentalOn, Long.parseLong(id), expectedReturnOn, stockId);

                    if (!(stockSum == stockNum)) {
                        String rentalError = "この期間は貸出できません";
                        result.addError(new FieldError("rentalManageDto", "expectedReturnOn", rentalError));
                        result.addError(new FieldError("rentalManageDto", "expectedRentalOn", rentalError));

                    }
                }
            }

            if (result.hasErrors()) {
                throw new Exception("Validation error.");
            }
            // 更新処理
            this.rentalManageService.update(Long.valueOf(id), rentalManageDto);

            return "redirect:/rental/index";
        } catch (Exception e) {
            log.error(e.getMessage());

            RentalManage rentalManage = this.rentalManageService.findById(Long.valueOf(id));

            // レコードのデータを挿入する箱を作る

            // 箱にデータを移す
            rentalManageDto.setId(rentalManage.getId());
            rentalManageDto.setEmployeeId(rentalManage.getAccount().getEmployeeId());
            rentalManageDto.setExpectedRentalOn(rentalManage.getExpectedRentalOn());
            rentalManageDto.setExpectedReturnOn(rentalManage.getExpectedReturnOn());
            rentalManageDto.setStatus(rentalManage.getStatus());
            rentalManageDto.setStockId(rentalManage.getStock().getId());

            // データを表示
            ra.addFlashAttribute("rentalManageDto", rentalManageDto);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.rentalManageDto", result);

            // エラーが発生したため元の入力フォームにリダイレクト

            return String.format("redirect:/rental/%s/edit", id);
        }
    }
}
