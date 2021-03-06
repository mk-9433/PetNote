package com.example.demo.app;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.app.account.AccountForm;
import com.example.demo.app.login.LoginForm;
import com.example.demo.app.pet.PetForm;
import com.example.demo.app.record.RecForm;
import com.example.demo.entity.Account;
import com.example.demo.entity.Login;
import com.example.demo.entity.Pet;
import com.example.demo.entity.Record;
import com.example.demo.service.AccountService;
import com.example.demo.service.LoginService;
import com.example.demo.service.PetService;
import com.example.demo.service.RecordService;

@Controller
@RequestMapping("/")
//@SessionAttributesは1つのController内で扱う複数のリクエスト間で
//データを共有する場合に有効。
//types属性にHTTPセッションに格納するオブジェクトクラスを指定する。
@SessionAttributes("loginForm")
public class PetNoteController {
	
    private final LoginService loginService;
    private final AccountService accountService;
    private final PetService petService;
    private final RecordService recordService;

    public PetNoteController(
    		LoginService loginService, 
    		AccountService accountService,
    		PetService petService,
    		RecordService recordService
    		) {
        this.loginService = loginService;
        this.accountService = accountService;
        this.petService = petService;
        this.recordService = recordService;
    }
    
    /*
     * オブジェクトをHTTPセッションに追加する
     */
    @ModelAttribute("loginForm")
    public LoginForm setUpLoginForm() {
    	LoginForm loginForm = new LoginForm();
        return loginForm;
    }
    
    @ModelAttribute("accountForm")
    public AccountForm setUpAccountForm() {
    	AccountForm accountForm = new AccountForm();
        return accountForm;
    }
    
    @ModelAttribute("petForm")
    public PetForm setUpPetForm() {
    	PetForm petForm = new PetForm();
        return petForm;
    }
    
    @ModelAttribute("recForm")
    public RecForm setUpRecForm() {
    	RecForm recForm = new RecForm();
        return recForm;
    }
    
	/////////////////////
	// 画像アップロード
	/////////////////////

	private String getUploadFileName(String fileName) {

		return DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now())
				//+ getExtension(fileName)
				+ "_" + fileName;
	}

	// 画像の分類用にcatNameを取得しフォルダ作成、既にフォルダがある場合は何もしない
	private String createAndReturnPath(String catName) {
		Path path = Paths.get("src/main/resources/static/images/" + catName + "/");
		if (!Files.exists(path)) {
			try {
				Files.createDirectory(path);
			} catch (Exception e) {
				// エラー処理は省略
			}
		}
		return path.toString();
	}
	
	//画像fileをimage/catNameに保存し、DBに保存するための画像のパスfilePathを返す
	private String saveAndGetFilePath(MultipartFile file, String catName) {
		String dirPath = createAndReturnPath(catName);
		
		String fileName = getUploadFileName(file.getOriginalFilename());
		Path filePath = Paths.get(dirPath + "/" + fileName);

		try (OutputStream os = Files.newOutputStream(filePath, StandardOpenOption.CREATE)) {
			byte[] bytes = file.getBytes();
			os.write(bytes);
			
		} catch (IOException e) {
			// エラー処理は省略
		}
		
		String relPath = "/images/" + catName + "/" + fileName;
		return relPath;
	}
	
	/////////////////////
	//最新loginFormの取得
	/////////////////////
	private LoginForm newLoginForm(String userName) {

		Account account = new Account();
		LoginForm loginForm = new LoginForm();
		
		//ログイン中のユーザーの、アカウント情報を取得		
		account = accountService.findByUserName(userName);
		
		//取得したaccount情報をloginFormにコピー
		BeanUtils.copyProperties(account, loginForm);
		
	    //petのリストを取得する
	    List<Pet> petList = petService.findByUserId(account.getUserId());
	    
	    //ペット一覧をペット名昇順にする設定。登録準の方が分かりやすいと思うのででコメントアウトしておく
	    //Collections.sort(petList);
	    
	    //成長記録を各petのrecListに代入
	    List<Record> recList = new ArrayList<Record>();
	    for ( Pet pet: petList ) {
	    	int petId = pet.getPetId();
	    	recList = recordService.findByPetId(petId);
	    	Collections.sort(recList);
	    	pet.setRecList(recList);
	    }
	    
	    //取得したpetListをloginFormにセット
	    loginForm.setPetList(petList);
		
	    return loginForm;
	    
	}
    
/////////////////////////////////////////	ページ遷移  /////////////////////	/////////////////////
	
	/////////////////////
	//TOPへ戻る
	/////////////////////
	@RequestMapping("/")
	public String goIndex(
			Model model
			) {
		model.addAttribute("title", "ようこそ");
		return "index";
	}
	
	@RequestMapping("/index")
	public String backToIndex(Model model) {
		model.addAttribute("title", "ようこそ");
		return "index";
	}
    
	/////////////////////	/////////////////////	Login  /////////////////////	/////////////////////
    
	/////////////////////
	//ログイン
	/////////////////////
	@RequestMapping("/login")
	public String goLogin(
			Model model
			) {
		//model.addAttribute("loginForm", loginForm);
//		if ( loginForm.getPass() == null ) {
			model.addAttribute("title", "ログイン");
			return "login";

//		} else {
//			model.addAttribute("title", "エラー:既にログインしています");
//			return "redirect:/error?loggedin_already";
//		}
	}
	
    //Modelから取得するオブジェクトの属性名は、@ModelAttributeのvalue属性に指定する。
    @RequestMapping(value = "/login_ok", method = RequestMethod.POST)
    public String loginCheck(
    		@Validated @ModelAttribute("loginForm") LoginForm loginForm, 
    		BindingResult resultLogin,
    		Model model) {
    	
    	// 入力値にエラーがある場合、自画面遷移する
		if (resultLogin.hasErrors()) {
			model.addAttribute("title", "ログイン");
			return "login";
		}
        
    	//ログイン認証結果をloginResultに代入
    	String userName = loginForm.getUserName();
    	String pass = loginForm.getPass();
    	Login login = new Login(userName, pass); 	
        boolean loginResult = loginService.execute(login);
            
        //ログイン認証OKの場合
        if(loginResult) {
        	
        	//ログインしたユーザーの、アカウント情報を取得
        	loginForm = newLoginForm(loginForm.getUserName());
        	
            //accountFormをセッションにセット
        	model.addAttribute("loginForm", loginForm);
        	
        	model.addAttribute("menuView", true);
        	model.addAttribute("title", "マイページ");
            return "redirect:/mypage/" + userName;
            
        } else {
        	model.addAttribute("title", "ログイン");
        	return "redirect:/login?error";
        }
    }

	/////////////////////
	//　マイページ表示
	/////////////////////
    //Modelから取得するオブジェクトの属性名は、@ModelAttributeのvalue属性に指定する。
    @RequestMapping(value = "/mypage/{userName}")
    public String goMyPage(
    		@PathVariable("userName") String userName,
    		LoginForm loginForm,
    		Model model) {
    	
    	if(loginForm.getPass() != null) {
    		
        	//セッションに入っているloginFormに、最新loginFormを上書き
    		//（ログインした人がログアウト手続きをせずに新規アカウントを作成した際に、前のユーザー名のマイページを表示させないため）
        	LoginForm newLoginForm = new LoginForm();
        	newLoginForm = newLoginForm(loginForm.getUserName());
        	model.addAttribute("loginForm", newLoginForm);
    		
	    	LocalDateTime dt = LocalDateTime.now();
	    	dt = dt.of(dt.getYear(), dt.getMonth(), dt.getDayOfMonth(), dt.getHour(), dt.getMinute(), dt.getSecond());
	    	
	    	model.addAttribute("now", dt);
	    	model.addAttribute("menuView", true);
	    	model.addAttribute("title", "マイページ");
	    	return "mypage";
    	} else {
	    	model.addAttribute("title", "エラー");
    		return "redirect:/error?login_required";
    	}
    }

    /////////////////////
	//　ログアウト
	/////////////////////
    
    @RequestMapping("/logout")
    public String goLogout(
    		LoginForm loginForm,
    		PetForm petForm,
    		SessionStatus sassionStatus,
    		Model model) {
    	
    	sassionStatus.setComplete();
    	
    	model.addAttribute("title", "ようこそ");
    	return "redirect:/index?logout";
    }
    
	/////////////////////	/////////////////////	Account  /////////////////////	/////////////////////
    
    
	/////////////////////
	// アカウント新規登録
	/////////////////////

	@RequestMapping(value="/register")
	public String registerAccount(
			AccountForm accountForm,
			LoginForm loginForm,
			Model model
			) {
		
//		if (loginForm.getPass().isEmpty()) {
			//アカウント新規登録
			model.addAttribute("accountForm", accountForm);
			
			model.addAttribute("title", "アカウント登録");
			return "register";
//		} else {
//			model.addAttribute("title", "エラー：既にログインしています");
//			return "redirect:/error?loggedin_already";
//		}
		
	}

	@RequestMapping(value = "/register_confirm", method = RequestMethod.POST)
	public String confirmRegisterAccount(
			@Validated @ModelAttribute("accountForm") AccountForm accountForm, 
			BindingResult resultAccount,
			Model model
			) {
		
		// エラーがある場合、自画面遷移する
    	if (resultAccount.hasErrors()) {
    		model.addAttribute("accountForm", accountForm);
			model.addAttribute("title", "アカウント登録");
    		return "register";
    	}		

		//画像を保存 ファイル名filePath で画像を保存,　空の場合はダミー画像
		String filePath = accountForm.getIcon();
		if ( accountForm.getIconFile().isEmpty() && filePath == null ) {
			filePath = "/images/dummy.jpg";
		} else if ( !accountForm.getIconFile().isEmpty() ){
			filePath = saveAndGetFilePath(accountForm.getIconFile(), "account");
		}

		accountForm.setIcon(filePath);
		model.addAttribute("accountForm", accountForm);
    	
    	//アカウント名重複確認
		if (accountService.isExist(accountForm.getUserName())) {
			model.addAttribute("userNameHasError", true);
			model.addAttribute("title", "入力内容確認");
			return "register_confirm";
			
		} else {
			
			model.addAttribute("title", "入力内容確認");
			return "register_confirm";			
		} 

	}

	@RequestMapping(value = "/register_complete", method = RequestMethod.POST)
	public String completeRegisterAccount(
			AccountForm accountForm, 
			LoginForm loginForm,
			Model model
			) {

		// 入力されたアカウント情報accountForm を accountに詰めなおす
		Account account = new Account();
    	BeanUtils.copyProperties(accountForm, account);

		// DBにaccountを登録
		accountService.registerAccount(account);

		//loginFormに新規登録されたアカウントのユーザー名とパスワードをセットする
		loginForm.setUserName(accountForm.getUserName());
		loginForm.setPass(accountForm.getPass());
		
		model.addAttribute("title", "アカウント登録完了");
		return "complete";
	}

	/////////////////////
	// アカウント情報表示
	/////////////////////

	@RequestMapping("/account_info/{userName}")
	public String viewAccountInfo(
			@PathVariable(name = "userName") String userName, 
			AccountForm accountForm,
			LoginForm loginForm,
			Model model
			) {
		
		if(loginForm.getPass() != null) {
	
			model.addAttribute("menuView", true);
			model.addAttribute("title", "アカウント情報確認");
			return "account_info";
			
		} else {
			model.addAttribute("title", "エラー");
			return "redirect:/error?login_required";
		}
	}

	/////////////////////
	// アカウント情報編集
	/////////////////////
	@RequestMapping(value="/account_edit/{userName}", method = RequestMethod.GET)
	public String editAccount(
			@PathVariable("userName") String userName,
			AccountForm accountForm,
			LoginForm loginForm,
			Model model
			) {

		if(loginForm.getPass() != null) {
			
			//userNameのaccount情報を見つける
			Account account = new Account();
			account = accountService.findByUserName(userName);
			
	    	//取得したaccount情報をaccountFormにコピー
	    	BeanUtils.copyProperties(account, accountForm);
	    	
			//詰めなおしたaccountFormをセッション(model)にセットする
			model.addAttribute("accountForm", accountForm);
			
			model.addAttribute("menuView", true);
			model.addAttribute("title", "アカウント情報編集");
			return "account_edit";
			
		} else {
			model.addAttribute("title", "エラー");
			return "redirect:/error?login_required";
		}
		
	}
	
    @RequestMapping(value="/account_edit_complete", method = RequestMethod.POST)
    public String completeEditAccount(
			@Validated @ModelAttribute("accountForm") AccountForm accountForm, 
			BindingResult resultAccount,
			LoginForm loginForm,
			Model model
    		) {
    	
		// エラーがある場合、自画面遷移する
    	if (resultAccount.hasErrors()) {
    		model.addAttribute("menuView", true);
    		model.addAttribute("title", "アカウント情報編集");
    		return "account_edit";
    	}
    	
    	// セッションスコープaccountFormをaccountにつめなおす
    	Account account = new Account();
    	BeanUtils.copyProperties(accountForm, account);
    	
		//画像の変更があった場合は、新しい画像を保存し、ファイル名パス filePath をaccountに上書き、空の場合は古いのをそのまま残す
    	String filePath = accountForm.getIcon();
		if ( !accountForm.getIconFile().isEmpty() ) {
			filePath = saveAndGetFilePath(accountForm.getIconFile(), "account");
		}
		account.setIcon(filePath);
		accountForm.setIcon(filePath);
		
    	// 変更したaccount情報をDBに上書き実行
    	accountService.editAccount(account);
    	
    	//セッションに入っているloginFormに、最新loginFormを上書き
    	LoginForm newLoginForm = new LoginForm();
    	newLoginForm = newLoginForm(loginForm.getUserName());
    	model.addAttribute("loginForm", newLoginForm);

		model.addAttribute("menuView", true);
    	model.addAttribute("title", "アカウント情報確認");
    	//url用にuserIdを取得
    	String userName = loginForm.getUserName();
    	String url = "redirect:/account_info/" + userName+ "?edit_complete";	
    	return url;    
    }
    
	/////////////////////	/////////////////////	Pet  /////////////////////	/////////////////////
    
    /////////////////////
	//　ペット追加登録
	/////////////////////
    
	@RequestMapping(value = "/pet_add")
	public String goAddPet(
			LoginForm loginForm,
			PetForm petForm, 
			Model model
			)  {
		
		if(loginForm.getPass() != null) {
			model.addAttribute("menuView", true);
			model.addAttribute("title", "ペット追加");
			return "pet_add";
			
    	} else {
	    	model.addAttribute("title", "エラー");
    		return "redirect:/error?login_required";
    	}
	}
 
    @RequestMapping(value = "/pet_confirm", method = RequestMethod.POST)
    public String confirmAddPet(
    		LoginForm loginForm,
    		@Validated @ModelAttribute("petForm") PetForm petForm, 
    		BindingResult resultPet,
    		Model model
    		) {
    	
    	// エラーがある場合、自画面遷移する
    	if (resultPet.hasErrors()) {
    		model.addAttribute("menuView", true);
    		model.addAttribute("title", "ペット追加");
    		return "pet_add";
    	}
    	
		//画像を保存 ファイル名filePath で画像を保存,　空の場合はダミー画像
		String filePath = petForm.getPetIcon();
		if ( petForm.getPetIconFile().isEmpty() && filePath == null ) {
			filePath = "/images/dummy.jpg";
		} else if ( !petForm.getPetIconFile().isEmpty() ){
			filePath = saveAndGetFilePath(petForm.getPetIconFile(), "pet");
		}
		
		petForm.setPetIcon(filePath);
		model.addAttribute("petForm", petForm);

		model.addAttribute("menuView", true);
		model.addAttribute("title", "入力内容確認");
    	return "pet_confirm";

    }
	
    @RequestMapping(value = "/pet_complete", method = RequestMethod.POST)
    public String completeAddPet(
    		PetForm petForm, 
    		LoginForm loginForm,
    		Model model
    		) {
        
        //入力されたPetForm を petに詰めなおす
    	Pet pet = new Pet();
    	BeanUtils.copyProperties(petForm, pet);

    	//DBにpetを登録
		petService.addPet(pet);
		
    	//セッションに入っているloginFormを最新のnewLoginFormに更新
		LoginForm newLoginForm = new LoginForm();
    	newLoginForm = newLoginForm(loginForm.getUserName());
    	model.addAttribute("loginForm", newLoginForm);
    	
		model.addAttribute("menuView", true);
		model.addAttribute("title", "ペット追加完了");
		return "complete";
    }
    
    
    /////////////////////
	//　ペット編集
	/////////////////////
    
    @RequestMapping(value = "/pet_edit", method = RequestMethod.POST)
    public String editPetForm(
    		LoginForm loginForm,
			PetForm petForm, 
			Model model
    		) {
    	
    	if( loginForm.getPass() != null ) {
			model.addAttribute("menuView", true);
	    	model.addAttribute("title", "ペット情報編集");
	    	return "pet_edit";
	    	
		} else {
	    	model.addAttribute("title", "エラー");
    		return "index";
		}
		
    }
    
    @RequestMapping(value = "/pet_edit_complete", method = RequestMethod.POST)
    public String completeEditPetForm(
    		LoginForm loginForm,
    		@Validated @ModelAttribute("petForm") PetForm petForm, 
    		BindingResult result, 
    		Model model
    		) {
    	
		// エラーがある場合、自画面遷移する
		if (result.hasErrors()) {
    		model.addAttribute("menuView", true);
			model.addAttribute("title", "ペット情報編集");
			return "pet_edit";
		}
		
    	// セッションスコープpetFormをpetにつめなおす
    	Pet pet = new Pet();
    	BeanUtils.copyProperties(petForm, pet);
    	
		//画像の変更があった場合は、新しい画像を保存し、ファイル名パス filePath をpetに上書き
    	String filePath = petForm.getPetIcon();
		if ( !petForm.getPetIconFile().isEmpty() ) {
			filePath = saveAndGetFilePath(petForm.getPetIconFile(), "pet");
		}
		pet.setPetIcon(filePath);
		
    	//DBにpetを登録
		petService.editPet(pet);
		
    	//セッションに入っているloginFormを最新のloginFormに更新
		LoginForm newLoginForm = new LoginForm();
    	newLoginForm = newLoginForm(loginForm.getUserName());
    	model.addAttribute("loginForm", newLoginForm);

		model.addAttribute("menuView", true);
		model.addAttribute("title", "ペット追加完了");
		return "complete";
    }
    
    /////////////////////
	//　ペット削除
	/////////////////////
    
    @RequestMapping(value = "/pet_delete_confirm", method = RequestMethod.POST)
    public String deletePetConfirm(
    		LoginForm loginForm,
    		@Validated @ModelAttribute("petForm") PetForm petForm, 
    		Model model,
    		BindingResult result
    		) {
    	
        model.addAttribute("petForm", petForm);
    	
		model.addAttribute("menuView", true);
        model.addAttribute("title", "ペット削除確認");
    	return "pet_delete_confirm";
    }
    
    @RequestMapping(value = "/pet_delete_complete", method = RequestMethod.POST)
    public String deletePetComplete(
    		LoginForm loginForm,
    		PetForm petForm, 
    		Model model
    		) {
        
        //入力されたPetFormから削除するpetIdを取り出す
    	int petId = petForm.getPetId();

    	//petIdのペットを削除
		petService.deleteByPetId(petId);
		
		//petIdに紐づく成長記録1が存在する場合、も全て削除
		List<Record> recList = recordService.findByPetId(petId);
		if(!recList.isEmpty()) {
			recordService.deleteByPetId(petId);
		}
			
    	//セッションに入っているaccountFormを最新のnewAccountFormに更新
    	LoginForm newLoginForm = new LoginForm();
    	newLoginForm = newLoginForm(loginForm.getUserName());
    	model.addAttribute("loginForm", newLoginForm);

		model.addAttribute("menuView", true);
		model.addAttribute("title", "ペット削除完了");
		return "complete";
    }
  
	/////////////////////	/////////////////////	Record  /////////////////////	/////////////////////
    
	/////////////////////
	//　成長記録表示
	/////////////////////
    @RequestMapping(value="/record_list/{petId}/{petName}")
    public String viewRecordList(
    		LoginForm loginForm,
    		@PathVariable("petId") int petId,
    		@PathVariable("petName") String petName,
    		PetForm petForm, 
    		//@RequestParam(value = "petId", defaultValue = "1") int petId,
//            @PageableDefault(
//                    page = 0,
//                    size = 5,
//                    sort = {"redId"},
//                    direction = Sort.Direction.DESC
//                    ) Pageable pageable,
    		Model model
    		) {
    	
		if(loginForm.getPass() != null) {
    	
	    	List<Record> recList = recordService.findByPetId(petId);
	    	
	    	petForm.setRecList(recList);
	    	petForm.setPetId(petId);
	    	petForm.setPetName(petName);
	    	model.addAttribute("petForm", petForm);
	    	
//	    	//成長記録をpage型に変換
//	    	Page<Record> recListPage = new PageImpl<Record>(recList, pageable, recList.size());
//	        model.addAttribute("page", recListPage);
//	        model.addAttribute("petId", petId);
	    
			model.addAttribute("menuView", true);
	    	model.addAttribute("title", "成長記録一覧");
	    	return "record_list";
		} else {
			model.addAttribute("title", "エラー");
			return "redirect:/error?login_required";
			
		}
    }
    
	/////////////////////
	// 成長記録投稿
	/////////////////////

	@RequestMapping(value = "/record_complete", method = RequestMethod.POST)
	public String postRecord(
			LoginForm loginForm,
			@Valid @ModelAttribute("recForm") RecForm recForm, 
			BindingResult resultRec, 
			Model model
			) {

		// エラーがある場合、自画面遷移する
		if (resultRec.hasErrors()) {
    		model.addAttribute("menuView", true);
			model.addAttribute("title", "マイページ");
			return "mypage";
		}
		
		//String→LocalDateTimeに日付フォーマットを修正
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
		LocalDateTime formattedDate = LocalDateTime.parse(recForm.getRecDate());
		
		//画像を保存 ファイル名filePath で画像を保存,　空の場合はnull
		String filePath;
		if ( recForm.getRecPicFile().isEmpty() ) {
			filePath = null;
		} else {
			filePath = saveAndGetFilePath(recForm.getRecPicFile(), "rec");
		}
		
		recForm.setRecPic(filePath);
		model.addAttribute("recForm", recForm);
		
        //入力されたRecForm を recordに詰めなおす
    	Record record = new Record();
    	record.setComment(recForm.getComment());
    	record.setRecDate(formattedDate);
    	record.setRecPic(filePath);
    	record.setPetId(recForm.getPetId());		
    	
    	//DBにrecordを登録
		recordService.postRecord(record);
		
    	//セッションに入っているaccountFormを最新のnewAccountFormに更新
    	LoginForm newLoginForm = new LoginForm();
    	newLoginForm = newLoginForm(loginForm.getUserName());
    	model.addAttribute("loginForm", newLoginForm);

		model.addAttribute("menuView", true);
		model.addAttribute("title", "成長記録投稿完了");
		return "complete";

	}
	

	
    /////////////////////
	//　成長記録編集
	/////////////////////
    
    @RequestMapping(value = "/record_edit", method = RequestMethod.POST)
    public String editRecForm(
    		LoginForm loginForm,
			RecForm recForm, 
			Model model
    		) {
		model.addAttribute("menuView", true);
    	model.addAttribute("title", "成長記録編集");
    	return "record_edit";
    }
    
    @RequestMapping(value = "/record_edit_complete", method = RequestMethod.POST)
    public String completeEditRecForm(
    		LoginForm loginForm,
    		@Validated @ModelAttribute("recForm") RecForm recForm, 
    		BindingResult result, 
    		Model model
    		) {
    	
		// エラーがある場合、自画面遷移する
		if (result.hasErrors()) {
    		model.addAttribute("menuView", true);
			model.addAttribute("title", "成長記録編集");
			return "record_edit";
		}
		
        //セッションスコープにあるRecForm を recordに詰めなおす
    	Record record = new Record();
    	BeanUtils.copyProperties(recForm, record);
    	
		//String→LocalDateTimeに日付フォーマットを修正
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
		LocalDateTime formattedDate = LocalDateTime.parse(recForm.getRecDate());
		
		//成長記録日を上書き
    	record.setRecDate(formattedDate);
		
		//filePathに既存の画像のパスを代入
		String filePath =  recForm.getRecPic();
		
		//画像の変更があった場合は、新しい画像を保存し、ファイル名パス filePath をrecordに上書き
		if ( recForm.getRecPicFile() != null ) {
			filePath = saveAndGetFilePath(recForm.getRecPicFile(), "rec");
		}
		record.setRecPic(filePath);

    	//DBにpetを登録
    	recordService.editRecord(record);
		
    	//セッションに入っているloginFormを最新のloginFormに更新
		LoginForm newLoginForm = new LoginForm();
    	newLoginForm = newLoginForm(loginForm.getUserName());
    	model.addAttribute("loginForm", newLoginForm);

		model.addAttribute("menuView", true);
		model.addAttribute("title", "成長記録編集完了");
		return "complete";
    }
    
	/////////////////////
	// 成長記録削除確認
	/////////////////////
	@RequestMapping(value = "/record_delete_confirm")
	public String confirmDeleteRecord(
			LoginForm loginForm,
			RecForm recForm,
			Model model
			) {
		if(loginForm.getPass() != null) {
			model.addAttribute("menuView", true);
			model.addAttribute("title", "成長記録削除確認");
			return "record_delete_confirm";
		} else {
			model.addAttribute("title", "エラー");
			return "redirect:/error?login_required";
			
		}
	}


	/////////////////////
	// 成長記録削除
	/////////////////////
	@RequestMapping(value = "/record_delete_complete", method = RequestMethod.POST)
	public String completeDeleteRecord(
			LoginForm loginForm,
			RecForm recForm,
			Model model
			) {
		
		if( loginForm.getPass() != null ) {
			
	        //入力されたRecFormから削除するpetIdを取り出す
	    	int recId = recForm.getRecId();
	
			//recIdの成長記録を削除
			recordService.deleteByRecId(recId);
			
	    	//セッションに入っているaccountFormを最新のnewAccountFormに更新
	    	LoginForm newLoginForm = new LoginForm();
	    	newLoginForm = newLoginForm(loginForm.getUserName());
	    	model.addAttribute("loginForm", newLoginForm);
	
			model.addAttribute("menuView", true);
			model.addAttribute("title", "成長記録削除完了");
			return "complete";
		} else {
			model.addAttribute("title", "エラー");
			return "redirect:/error?login_required";
			
		}
	}
	
	/////////////////////
	//　成長記録表示
	/////////////////////
    @RequestMapping(value="/record_list_test")
    public String viewRecordList(
    		LoginForm loginForm,
    		PetForm petForm, 
    		@RequestParam(value = "petId", defaultValue = "1") int petId,
            @PageableDefault(
                    page = 0,
                    size = 5,
                    sort = {"redId"},
                    direction = Sort.Direction.DESC
                    ) Pageable pageable,
    		Model model
    		) {
    	
	    	List<Record> recList = recordService.findByPetId(petId);
	    	
	    	petForm.setRecList(recList);
	    	petForm.setPetId(petId);
	    	petForm.setPetName(petService.findByPetId(petId).getPetName());
	    	model.addAttribute("petForm", petForm);
	    	
	    	//成長記録をpage型に変換
	    	Page<Record> recListPage = new PageImpl<Record>(recList, pageable, recList.size());
	        model.addAttribute("page", recListPage);
	        model.addAttribute("petId", petId);
	    
			model.addAttribute("menuView", true);
	    	model.addAttribute("title", "成長記録一覧");
	    	return "record_list";

    }
    
}
