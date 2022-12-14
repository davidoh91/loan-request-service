package com.neo.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.LocaleResolver;

import com.neo.common.vo.DemoFileVO;
import com.neo.common.vo.DemoVO;
import com.neo.common.vo.FileVO;
import com.neo.common.vo.MasterKeyVO;
import com.neo.mappers.CommonMapper;
import com.neo.mappers.DemoMapper;
import com.neo.util.UtilCommon;
import com.neo.util.UtilDate;
import com.neo.util.UtilFile;
import com.neo.util.UtilJsonResult;

@Service("demoService")
@Transactional
public class DemoServiceImpl implements DemoService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired MessageSource messageSource;
	@Autowired LocaleResolver localeResolver;
	@Resource(name="demoMapper")
	private DemoMapper demoMapper;
	@Resource(name="commonMapper")
	private CommonMapper commonMapper;
	
	@Value("${file.path.upload}") String FILE_PATH_UPLOAD;
	@Value("${file.ext.board}") String FILE_EXT;
	@Value("${file.store.demo}") String FILE_STORE_PATH;
	@Value("${file.group.demo}") String FILE_UPLOAD_GROUP;
	
	@Override
	public int demoListCount(DemoVO paramVO) throws Exception {
		return demoMapper.demoListCount(paramVO);
	}
	
	@Override
	public List<DemoVO> demoList(DemoVO paramVO) throws Exception {
		List<DemoVO> resultList = new ArrayList<>();
		resultList = demoMapper.demoList(paramVO);
		return resultList;
	}

	@Override
	public DemoVO demoDetail(DemoVO paramVO) throws Exception {
		DemoVO result = new DemoVO();
		result = demoMapper.demoDetail(paramVO);
		
		// ????????????????????? html ????????? unescape ??????
		result.setDEMO_CONTENTS(StringEscapeUtils.unescapeHtml4(result.getDEMO_CONTENTS()));
		
		return result;
	}

	@Override
	public JSONObject demoInsert(DemoVO paramVO, HttpServletRequest request) throws Exception {
		JSONObject json = new JSONObject();
		UtilJsonResult.setReturnCodeFail(json);
		String rMsg = messageSource.getMessage("error.system.default", null, localeResolver.resolveLocale(request));

		// S:???????????????
		// ?????? ??????
		if(UtilCommon.isEmpty(paramVO.getDEMO_TITLE())) {
			rMsg = messageSource.getMessage("validation.empty.input", new String[] {"???????????????"}, localeResolver.resolveLocale(request)); 
			UtilJsonResult.setReturnCodeFail(json, rMsg);
			return json;
			
		}
		// E:???????????????
		
		// S: ?????? ??????
		String masterKey = "";
		MasterKeyVO masterKeyVO = new MasterKeyVO();		
		masterKeyVO.setKEY_GBN("DEMO_CODE");
		masterKey = commonMapper.getFnGetMasterKey(masterKeyVO);
		paramVO.setDEMO_CODE(masterKey);
		// E: ?????? ??????
		
		paramVO.setIN_USER(paramVO.getLoginId());
		paramVO.setUP_USER(paramVO.getLoginId());
		demoMapper.demoInsert(paramVO);

		UtilJsonResult.setReturnCodeSuc(json);
		
		return json;
	}

	@Override
	public JSONObject demoUpdate(DemoVO paramVO, HttpServletRequest request) throws Exception {
		JSONObject json = new JSONObject();
		UtilJsonResult.setReturnCodeFail(json);
		String rMsg = messageSource.getMessage("error.system.default", null, localeResolver.resolveLocale(request));
		
		// S:???????????????
		if(UtilCommon.isEmpty(paramVO.getDEMO_TITLE())) {
			rMsg = messageSource.getMessage("validation.empty.input", new String[] {"???????????????"}, localeResolver.resolveLocale(request)); 
			UtilJsonResult.setReturnCodeFail(json, rMsg);
			return json;
		}
		if(UtilCommon.isEmpty(demoMapper.demoDetail(paramVO))) {
			rMsg = messageSource.getMessage("validation.update.empty.object", null, localeResolver.resolveLocale(request)); 
			UtilJsonResult.setReturnCodeFail(json, rMsg);
			return json;
		}
		// E:???????????????
		
		paramVO.setUP_USER(paramVO.getLoginId());
		demoMapper.demoUpdate(paramVO);
		
		UtilJsonResult.setReturnCodeSuc(json);
		
		return json;
	}

	@Override
	public JSONObject demoDelete(DemoVO paramVO, HttpServletRequest request) throws Exception {
		JSONObject json = new JSONObject();
		UtilJsonResult.setReturnCodeFail(json);
		String rMsg = messageSource.getMessage("error.system.default", null, localeResolver.resolveLocale(request));
		
		// S:???????????????
		if(UtilCommon.isEmpty(demoMapper.demoDetail(paramVO))) {
			rMsg = messageSource.getMessage("validation.delete.empty.object", null, localeResolver.resolveLocale(request)); 
			UtilJsonResult.setReturnCodeFail(json, rMsg);
			return json;
		}
		// E:???????????????
		
		paramVO.setDEL_YN("Y");
		paramVO.setUP_USER(paramVO.getLoginId());
		demoMapper.demoDelete(paramVO);
		
		// S:??????????????????
		DemoFileVO paramFileVO = new DemoFileVO();
		List<DemoFileVO> resultListFile = new ArrayList<DemoFileVO>();
		paramFileVO.setDEMO_CODE(paramVO.getDEMO_CODE());
		resultListFile = demoMapper.demoFileList(paramFileVO);
		if(UtilCommon.isNotEmpty(resultListFile)) {
			demoMapper.demoFileDelete(paramFileVO);
			// ??????????????????
			for(DemoFileVO item: resultListFile) {
				UtilFile.deleteFileInf(item.getSYS_FILE_NAME(), FILE_PATH_UPLOAD, item.getUPLOAD_PATH());
			}
		}
		// E:??????????????????
		
		UtilJsonResult.setReturnCodeSuc(json);
		
		return json;
	}

	@Override
	public JSONObject demoInsert(DemoVO paramVO, MultipartHttpServletRequest request) throws Exception {
		JSONObject json = new JSONObject();
		UtilJsonResult.setReturnCodeFail(json);
		String rMsg = messageSource.getMessage("error.system.default", null, localeResolver.resolveLocale(request));
		
		// S:???????????????
		// ?????? ??????
		if(UtilCommon.isEmpty(paramVO.getDEMO_TITLE())) {
			rMsg = messageSource.getMessage("validation.empty.input", new String[] {"???????????????"}, localeResolver.resolveLocale(request));
			UtilJsonResult.setReturnCodeFail(json, rMsg);
			return json;
			
		}
		// E:???????????????
		
		// S: ?????? ??????
		String masterKey = "";
		MasterKeyVO masterKeyVO = new MasterKeyVO();		
		masterKeyVO.setKEY_GBN("DEMO_CODE");
		masterKey = commonMapper.getFnGetMasterKey(masterKeyVO);
		paramVO.setDEMO_CODE(masterKey);
		// E: ?????? ??????
		
		// S:??????????????? ??????
		Map<String, MultipartFile> mFiles = request.getFileMap();
		List<FileVO> files = new ArrayList<FileVO>();
		if(UtilCommon.isEmpty(mFiles)) {
//			rMsg = messageSource.getMessage("validation.empty.input", new String[] {"??????"}, localeResolver.resolveLocale(request));
//			UtilJsonResult.setReturnCodeFail(json, rMsg);
//			return json;
		}else {
			//?????? ?????? ??????
			files = UtilFile.parseFileInf(mFiles, UtilCommon.getRandomStr(5, "_"), FILE_PATH_UPLOAD, FILE_STORE_PATH);
			boolean extChkResult = false;
			if(files != null && !files.isEmpty()){
				for(int n=0; n<files.size(); n++) {
					FileVO extFileChk = files.get(n);
					String extChk = extFileChk.getFILE_EXT();
					if(FILE_EXT.indexOf(extChk.toUpperCase()) < 0) {
						extChkResult = false;
						break;
					}else {
						extChkResult = true;
					}
				}
			}
			
			if(!extChkResult) {
				for(int m=0; m<files.size(); m++) {
					FileVO extFileChk = files.get(m);
					logger.info("???????????? ?????? ?????? ??????????????? ????????? ????????? ???????????????.");
					UtilFile.deleteFileInf(extFileChk.getSYS_FILE_NAME(), FILE_PATH_UPLOAD, FILE_STORE_PATH + "/" +UtilDate.getDateFormatt("yyyy"));
				}
				
				rMsg = messageSource.getMessage("validation.file.availableExt", null, localeResolver.resolveLocale(request));
				UtilJsonResult.setReturnCodeFail(json, rMsg);
				return json;
			}
			
			// ????????????
			DemoFileVO paramVO_file;
			for(FileVO item : files) {
				paramVO_file = new DemoFileVO();
				// ????????? ????????? ????????????
				paramVO_file.setDEMO_CODE(paramVO.getDEMO_CODE());
				paramVO_file.setUPLOAD_GROUP(FILE_UPLOAD_GROUP);
				paramVO_file.setUPLOAD_PATH(FILE_STORE_PATH + item.getUPLOAD_PATH());
				paramVO_file.setORG_FILE_NAME(item.getORG_FILE_NAME());
				paramVO_file.setSYS_FILE_NAME(item.getSYS_FILE_NAME());
				paramVO_file.setFILE_EXT(item.getFILE_EXT());
				paramVO_file.setFILE_SIZE(item.getFILE_SIZE());
				paramVO_file.setIN_USER(paramVO.getLoginId());
				demoMapper.demoFileInsert(paramVO_file);
			}
		}
		// E:??????????????? ??????
		
		// ????????????
		paramVO.setIN_USER(paramVO.getLoginId());
		paramVO.setUP_USER(paramVO.getLoginId());
		demoMapper.demoInsert(paramVO);
		
		UtilJsonResult.setReturnCodeSuc(json, rMsg);
		
		return json;
	}

	@Override
	public JSONObject demoUpdate(DemoVO paramVO, MultipartHttpServletRequest request) throws Exception {
		JSONObject json = new JSONObject();
		UtilJsonResult.setReturnCodeFail(json);
		String rMsg = messageSource.getMessage("error.system.default", null, localeResolver.resolveLocale(request));
		
		// S:???????????????
		if(UtilCommon.isEmpty(paramVO.getDEMO_TITLE())) {
			rMsg = messageSource.getMessage("validation.empty.input", new String[] {"???????????????"}, localeResolver.resolveLocale(request)); 
			UtilJsonResult.setReturnCodeFail(json, rMsg);
			return json;
		}
		if(UtilCommon.isEmpty(demoMapper.demoDetail(paramVO))) {
			rMsg = messageSource.getMessage("validation.update.empty.object", null, localeResolver.resolveLocale(request)); 
			UtilJsonResult.setReturnCodeFail(json, rMsg);
			return json;
		}
		// E:???????????????
		
		paramVO.setUP_USER(paramVO.getLoginId());
		demoMapper.demoUpdate(paramVO);
		
		// S:??????????????? ??????
		Map<String, MultipartFile> mFiles = request.getFileMap();
		List<FileVO> files = new ArrayList<FileVO>();
		if(UtilCommon.isEmpty(mFiles)) {
//			rMsg = messageSource.getMessage("validation.empty.input", new String[] {"??????"}, localeResolver.resolveLocale(request));
//			UtilJsonResult.setReturnCodeFail(json, rMsg);
//			return json;
		}else {
			
			// S:??????????????????
			// ????????????????????? ?????????????????? ??????????????? ???????????? ?????? ????????? ???????????? ????????? ???????????????
			// ????????? ?????????????????? ?????? ???????????? ???????????????
			DemoFileVO paramFileVO = new DemoFileVO();
			List<DemoFileVO> resultListFile = new ArrayList<DemoFileVO>();
			paramFileVO.setDEMO_CODE(paramVO.getDEMO_CODE());
			resultListFile = demoMapper.demoFileList(paramFileVO);
			if(UtilCommon.isNotEmpty(resultListFile)) {
				demoMapper.demoFileDelete(paramFileVO);
				// ??????????????????
				for(DemoFileVO item: resultListFile) {
					UtilFile.deleteFileInf(item.getSYS_FILE_NAME(), FILE_PATH_UPLOAD, item.getUPLOAD_PATH());
				}
			}
			// E:??????????????????
			
			
			//?????? ?????? ??????
			files = UtilFile.parseFileInf(mFiles, UtilCommon.getRandomStr(5, "_"), FILE_PATH_UPLOAD, FILE_STORE_PATH);
			boolean extChkResult = false;
			if(files != null && !files.isEmpty()){
				for(int n=0; n<files.size(); n++) {
					FileVO extFileChk = files.get(n);
					String extChk = extFileChk.getFILE_EXT();
					if(FILE_EXT.indexOf(extChk.toUpperCase()) < 0) {
						extChkResult = false;
						break;
					}else {
						extChkResult = true;
					}
				}
			}
			
			if(!extChkResult) {
				for(int m=0; m<files.size(); m++) {
					FileVO extFileChk = files.get(m);
					logger.info("???????????? ?????? ?????? ??????????????? ????????? ????????? ???????????????.");
					UtilFile.deleteFileInf(extFileChk.getSYS_FILE_NAME(), FILE_PATH_UPLOAD, FILE_STORE_PATH + "/" +UtilDate.getDateFormatt("yyyy"));
				}
				
				rMsg = messageSource.getMessage("validation.file.availableExt", null, localeResolver.resolveLocale(request));
				UtilJsonResult.setReturnCodeFail(json, rMsg);
				return json;
			}
			
			// ????????????
			DemoFileVO paramVO_file;
			for(FileVO item : files) {
				paramVO_file = new DemoFileVO();
				// ????????? ????????? ????????????
				paramVO_file.setDEMO_CODE(paramVO.getDEMO_CODE());
				paramVO_file.setUPLOAD_GROUP(FILE_UPLOAD_GROUP);
				paramVO_file.setUPLOAD_PATH(FILE_STORE_PATH + item.getUPLOAD_PATH());
				paramVO_file.setORG_FILE_NAME(item.getORG_FILE_NAME());
				paramVO_file.setSYS_FILE_NAME(item.getSYS_FILE_NAME());
				paramVO_file.setFILE_EXT(item.getFILE_EXT());
				paramVO_file.setFILE_SIZE(item.getFILE_SIZE());
				paramVO_file.setIN_USER(paramVO.getLoginId());
				demoMapper.demoFileInsert(paramVO_file);
			}
		}
		// E:??????????????? ??????
		
		
		
		UtilJsonResult.setReturnCodeSuc(json);
		
		return json;
	}
	
	@Override
	public List<DemoFileVO> demoFileList(DemoFileVO paramVO) throws Exception {
		List<DemoFileVO> resultListFile = new ArrayList<DemoFileVO>();
		resultListFile = demoMapper.demoFileList(paramVO);
		return resultListFile;
	}

	@Override
	public DemoFileVO demoFileDetail(DemoFileVO paramVO) throws Exception {
		return demoMapper.demoFileDetail(paramVO);
	}

}
