

package com.neo.security;

import com.neo.common.vo.ConsultMemberVO;
import com.neo.common.vo.MemberVO;
import com.neo.mappers.ConsultMemberMapper;
import com.neo.mappers.MemberMapper;
import com.neo.util.UtilCommon;
import com.neo.util.UtilCrypt;
import com.neo.util.UtilOtp;
import com.penta.scpdb.ScpDbAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.LocaleResolver;

import javax.annotation.Resource;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

@Component
public class AuthProvider extends HttpServlet implements AuthenticationProvider {

    private static final long serialVersionUID = 6752294248872104141L;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    MessageSource messageSource;
    @Autowired
    LocaleResolver localeResolver;

    @Value("${crypt.privatekey.sha256}")
    String CRYPT_PRIVATEKEY_SHA256;

	@Value("${mastercode.member_gubun.admin}") String MASTERCODE_MEMBER_GUBUN_ADMIN;
	@Value("${mastercode.member_gubun.member}") String MASTERCODE_MEMBER_GUBUN_MEMBER;
    @Value("${mastercode.member_gubun.consult}") String MASTERCODE_MEMBER_GUBUN_CONSULT;

	@Value("${mastercode.member_authority.branch_member}") String MASTERCODE_MEMBER_AUTHORITY_BRANCH_MEMBER;
	@Value("${mastercode.member_authority.consult_member}") String MASTERCODE_MEMBER_AUTHORITY_CONSULT_MEMBER;
	@Value("${mastercode.member_authority.consult_admin}") String MASTERCODE_MEMBER_AUTHORITY_CONSULT_ADMIN;
	@Value("${mastercode.member_authority.system_admin}") String MASTERCODE_MEMBER_AUTHORITY_SYSTEM_ADMIN;

    @Value("${mastercode.platform_gubun.pc}")
    String MASTERCODE_PLATFORM_GUBUN_PC;
    @Value("${mastercode.platform_gubun.mobile}")
    String MASTERCODE_PLATFORM_GUBUN_MOBILE;

    //damo key
    @Value("${damo.inifilepath}")
    String iniFilePath;

    @Resource(name = "consultMemberMapper")
    private ConsultMemberMapper consultMemberMapper;
	@Resource(name="memberMapper")
	private MemberMapper memberMapper;
	
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String member_gubun = request.getParameter("member_gubun");            // ????????????: member, admin
        String platform_gubun = request.getParameter("platform_gubun");    // ???????????????: pc, mobile

        String rMsg = messageSource.getMessage("error.system.default", null, localeResolver.resolveLocale(request));

        Enumeration<String> params = request.getParameterNames();
        logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        while (params.hasMoreElements()) {
            String string = params.nextElement();
            logger.info(string + ":" + request.getParameter(string));
        }
        logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        logger.info("ip?????? ??????::::::::::::");
        String connIp = request.getRemoteHost();

        
        //?????? IP??????
//	    if(!IPCheck(connIp)){
//	    	rMsg = messageSource.getMessage("login.fail.ipCheck", null, localeResolver.resolveLocale(request)); 
//			throw new BadCredentialsException(rMsg);				    	
//	    }

        logger.info("member_gubun::::::::::::" + member_gubun);
        
        if (UtilCommon.isEmpty(member_gubun)) {
            return authentication;
        } else {

            String member_id = authentication.getName();
            String member_pw = authentication.getCredentials().toString();
            
            String enc_member_pw = ""; 
            		


            System.out.println("*************** test ******************");

            MemberVO paramVO = new MemberVO();
			MemberVO result_detail = new MemberVO();
            ConsultMemberVO consultParamVO = new ConsultMemberVO();
            ConsultMemberVO consult_result = new ConsultMemberVO();

			// ?????? ?????????
			if (MASTERCODE_MEMBER_GUBUN_MEMBER.equals(member_gubun)) {
				try {
					
					member_id = authentication.getName();
					member_pw = authentication.getCredentials().toString();
					enc_member_pw = UtilCrypt.hashSHA256(member_pw, CRYPT_PRIVATEKEY_SHA256);
					
					paramVO.setMEMBER_ID(member_id);
					
					// ???????????????
					result_detail = memberMapper.memberLogin(paramVO);

					// S: ???????????????
					// ??????????????????
					if(UtilCommon.isEmpty(result_detail)) {
						rMsg = messageSource.getMessage("login.fail.notFound", null, localeResolver.resolveLocale(request)); 
						throw new BadCredentialsException(rMsg);
					}
					// ???????????? ??????
					if(!enc_member_pw.equals(result_detail.getMEMBER_PW())) {
						rMsg = messageSource.getMessage("login.fail.missmatchPassword", null, localeResolver.resolveLocale(request)); 
						throw new BadCredentialsException(rMsg);
					}
					// S: ???????????????
					
					// ????????? ???????????? ?????? ==================================================================
					
					// ????????? ????????? ??????
					List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
					if(MASTERCODE_PLATFORM_GUBUN_PC.equals(platform_gubun)){
						roles.add(new SimpleGrantedAuthority("ROLE_MEMBER"));
					}else {
						roles.add(new SimpleGrantedAuthority("ROLE_MOBILE"));
					}
					UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(member_id, member_pw, roles);

					// ????????? ????????? ????????????
					result.setDetails(new CustomMemberDetails(
							result_detail.getMEMBER_CODE()
							, result_detail.getMEMBER_ID()
							, result_detail.getMEMBER_NAME()
							, platform_gubun
					));
					return result;

				} catch (DataAccessException e) {
					rMsg = messageSource.getMessage("error.database.connection", null, localeResolver.resolveLocale(request));
					throw new BadCredentialsException(rMsg);
				} catch (Exception ex) {
					String exceptionMsg = ex.getMessage();
					request.setAttribute("member_id", member_id);
					request.setAttribute("exceptionMsg", exceptionMsg);
					
					ex.printStackTrace();
					throw new BadCredentialsException(exceptionMsg);
				}
				
			// ??????????????????
			} else if (MASTERCODE_MEMBER_GUBUN_ADMIN.equals(member_gubun)) {
				
				try {
					member_id = authentication.getName();
					member_pw = authentication.getCredentials().toString();
					enc_member_pw = UtilCrypt.hashSHA256(member_pw, CRYPT_PRIVATEKEY_SHA256);
					
					paramVO.setMEMBER_ID(member_id);
					// ???????????????
					result_detail = memberMapper.memberLogin(paramVO);
					
					// S: ???????????????
					// ??????????????????
					if(UtilCommon.isEmpty(result_detail)) {
						rMsg = messageSource.getMessage("login.fail.notFound", null, localeResolver.resolveLocale(request)); 
						throw new BadCredentialsException(rMsg);
					}
					// ???????????? ??????
					if(!enc_member_pw.equals(result_detail.getMEMBER_PW())) {
						rMsg = messageSource.getMessage("login.fail.missmatchPassword", null, localeResolver.resolveLocale(request)); 
						throw new BadCredentialsException(rMsg);
					}
					// ????????? ????????? ????????? ??????
					if("MC0000700003".equals(result_detail.getMEMBER_STATUS())) {
						rMsg = messageSource.getMessage("login.fail.vanUser", null, localeResolver.resolveLocale(request)); 
						throw new BadCredentialsException(rMsg);
					}
					// S: ???????????????
					
					// ????????? ???????????? ?????? ==================================================================
					
					
					List<String> menuAuths = new ArrayList<String>();
					
					String platform_auth = "";
					
					if(UtilCommon.isNotEmpty(result_detail)) {
						if("1".equalsIgnoreCase(result_detail.getACCOUNT_USE())) {
							menuAuths.add("ACCOUNT_USE");
						}
						if("1".equalsIgnoreCase(result_detail.getTHANKS_USE())) {
							menuAuths.add("THANKS_USE");
						}
						if("1".equalsIgnoreCase(result_detail.getNEWS_USE())) {
							menuAuths.add("NEWS_USE");
						}
						if("1".equalsIgnoreCase(result_detail.getFINAN_USE())) {
							menuAuths.add("FINAN_USE");
						}
						if("1".equalsIgnoreCase(result_detail.getREPORT_USE())) {
							menuAuths.add("REPORT_USE");
						}
						for(int i=0; i<menuAuths.size(); i++) {
							
							platform_auth += menuAuths.get(i);
							if(i != (menuAuths.size() -1)){
								platform_auth += ",";
							}
						}
					}
					
					// ????????? ????????? ????????????
					List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
					roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
					UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(member_id, member_pw, roles);

					result.setDetails(new CustomAdminDetails(
							result_detail.getMEMBER_CODE()
							, result_detail.getMEMBER_ID()
							, result_detail.getMEMBER_NAME()
							, platform_gubun
							, platform_auth
					));
					return result;

				} catch (DataAccessException e) {
					rMsg = messageSource.getMessage("error.database.connection", null, localeResolver.resolveLocale(request));
					throw new BadCredentialsException(rMsg);
				} catch (Exception ex) {
					String exceptionMsg = ex.getMessage();
					request.setAttribute("member_id", member_id);
					request.setAttribute("exceptionMsg", exceptionMsg);
					
					ex.printStackTrace();
					throw new BadCredentialsException(rMsg);
				}
				
			// ????????? ?????????
			}else if (MASTERCODE_MEMBER_GUBUN_CONSULT.equals(member_gubun)) {
	            try {
	                member_id = authentication.getName();
	                member_pw = authentication.getCredentials().toString();
	                
	                logger.info("????????? ??????::::::::::::"+iniFilePath);
	                
	                //????????? ????????? ??????
	                ScpDbAgent agt = new ScpDbAgent();
	                
	                //logger.info("[java] member_pw :::::::::::::::::::::::::::: " + member_pw);
	                enc_member_pw = agt.ScpEncStr(iniFilePath, "KEY1", member_pw);    //?????????
	                logger.info("[java] ScpEncStr :::::::::::::::::::::::::::: " + enc_member_pw);
	                // ???????????????
	                consultParamVO.setMEMBER_ID(member_id);
	                
	                consult_result = consultMemberMapper.consultMemberLogin(consultParamVO);
	            	
	                //logger.info("[java] getMEMBER_PW :::::::::::::::::::::::::::: " + consult_result.getMEMBER_PW());
	                
	                
	                // S: ???????????????
	                // ??????????????????
	                if (UtilCommon.isEmpty(consult_result)) {
	                    rMsg = messageSource.getMessage("login.fail.notFound", null, localeResolver.resolveLocale(request));
	                    throw new BadCredentialsException(rMsg);
	                }

	                // ???????????? ??????
	                if (!enc_member_pw.equals(consult_result.getMEMBER_PW())) {
	                    rMsg = messageSource.getMessage("login.fail.missmatchPassword", null, localeResolver.resolveLocale(request));
	                    throw new BadCredentialsException(rMsg);
	                }
	                
	                // ???????????? ?????? ?????????
	                if ("MC0000700001".equals(consult_result.getMEMBER_STATUS())) {
	                	rMsg = messageSource.getMessage("login.fail.notConfirmUser", null, localeResolver.resolveLocale(request));
	                	throw new BadCredentialsException(rMsg);
	                // ???????????? ???????????????
	                }else if("MC0000700003".equals(consult_result.getMEMBER_STATUS())) {
	                	rMsg = messageSource.getMessage("login.fail.vanUser", null, localeResolver.resolveLocale(request));
	                	throw new BadCredentialsException(rMsg);
	                }
	                
	                
	                /**otp ?????? start**/
	                String member_otp = request.getParameter("MEMBER_OTP");    // otp key
	                
	                //logger.info("[java] member_otp :::::::::::::::::::::::::::: " + member_otp);
	                //logger.info("[java] db_member_otp :::::::::::::::::::::::::::: " + consult_result.getMEMBER_OTP());
	                
	                long t = new Date().getTime();
	                long ltime =  t / 30000;
	                 
	                boolean check_code = false;
                    // ???, ??????, ???????????? ????????? ??????????????? ????????? ?????? ?????? ??????.
                    check_code = UtilOtp.check_code(consult_result.getMEMBER_OTP(), member_otp, ltime);
                    
                    // otp ???????????? ??????
                    if(!check_code) {
                    	rMsg = messageSource.getMessage("login.fail.missmatchOtp", null, localeResolver.resolveLocale(request));
	                	throw new BadCredentialsException(rMsg);
                    }
                    /**otp ?????? end**/	
	                
	                // ????????? ????????? ????????? ??????
	                if ("MC0000700003".equals(consult_result.getMEMBER_STATUS())) {
	                    rMsg = messageSource.getMessage("login.fail.vanUser", null, localeResolver.resolveLocale(request));
	                    throw new BadCredentialsException(rMsg);
	                }
	                // S: ???????????????

	                // ????????? ???????????? ?????? ==================================================================


	                // ????????? ????????? ????????????
	                List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
	                roles.add(new SimpleGrantedAuthority("ROLE_CONSULT"));
	                UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(member_id, member_pw, roles);

	                // ????????? ????????? ?????? ???????????? ????????????????????? ????????????
	                consultParamVO.setMEMBER_CODE(consult_result.getMEMBER_CODE());
	                consultParamVO.setUP_USER(consult_result.getMEMBER_CODE());
	                consultMemberMapper.memberLastLoginDttmUpdate(consultParamVO);
	                
	                // ????????? ??? ip ?????????
	                consultParamVO.setMEMBER_ID(consult_result.getMEMBER_ID());
	                consultParamVO.setMEMBER_NAME(consult_result.getMEMBER_NAME());
	                
	                InetAddress inetAddr = InetAddress.getLocalHost();
	                String ipAddr = inetAddr.getHostAddress();
	                
	                consultParamVO.setCONN_IP(ipAddr);
	                consultMemberMapper.memberLoginLog(consultParamVO);
	                
	                /*  ************* ???????????? ?????? *************  :

	                    ???????????? ????????? MEMBER_AUTHORITY ??? ??????
	                    GNB ????????????????????? ?????????
	                    session ??? ????????????

	                    ?????? ?????? ?????? ?????? ?????? (consult/lnb.jsp ??????)
	                    -------------------------------------------------------------------------------------------
	                                ?????? ??????           |            ???????????? GNB ??????
	                    MC0002300001  ??????(?????????)      |     ??????????????????, 		???????????????, 			???????????????
	                    MC0002300002  ?????????           |     ??????????????????, 		???????????????, 			???????????????
	                    MC0002300003  ????????? ?????????     |     		??????????????????, ???????????????, 			???????????????
	                    MC0002300004  ????????? ?????????     |     		??????????????????, ???????????????, ???????????????
	                 */

	                String MEMBER_AUTHORITY = consult_result.getMEMBER_AUTHORITY();

	                // ????????? ????????? ????????? ???????????? ???????????? ????????? ????????????.
	                List<String> menuAuths = new ArrayList<String>();
	                String platform_auth = "";

	                HttpSession session = request.getSession();

	                if (MASTERCODE_MEMBER_AUTHORITY_BRANCH_MEMBER.equals(MEMBER_AUTHORITY)) {
	                    menuAuths.add("/consult/consult/requestList");		//??????????????????
	                    //menuAuths.add("/consult/board/confirm/confirmList");	//????????????????????????
	                    //menuAuths.add("/consult/board/sales/salesList");		//????????????
	                    menuAuths.add("/consult/user/profile");			//???????????????
	                } else if (MASTERCODE_MEMBER_AUTHORITY_CONSULT_MEMBER.equals(MEMBER_AUTHORITY)) {
	                    menuAuths.add("/consult/consult/requestList");		//??????????????????
	                    //menuAuths.add("/consult/board/confirm/confirmList");	//????????????????????????
	                    //menuAuths.add("/consult/board/salesLis");				//????????????
	                    menuAuths.add("/consult/user/profile");			//???????????????
	                } else if (MASTERCODE_MEMBER_AUTHORITY_CONSULT_ADMIN.equals(MEMBER_AUTHORITY)) {
	                    //menuAuths.add("/consult/consult/requestList");		//??????????????????
	                    //menuAuths.add("/consult/board/confirm/confirmList");	//????????????????????????
	                    //menuAuths.add("/consult/board/sales/salesList");		//????????????
	                    menuAuths.add("/consult/consult/consultList");		//??????????????? ??????
	                    menuAuths.add("/consult/consult/consultantList");		//???????????????
	                    menuAuths.add("/consult/consult/connList");			//????????????
	                    menuAuths.add("/consult/user/profile");			//???????????????
	                } else if (MASTERCODE_MEMBER_AUTHORITY_SYSTEM_ADMIN.equals(MEMBER_AUTHORITY)) {
	                    //menuAuths.add("/consult/consult/requestList");		//??????????????????
	                    //menuAuths.add("/consult/board/confirm/confirmList");	//????????????????????????
	                    //menuAuths.add("/consult/board/sales/salesList");		//????????????
	                    menuAuths.add("/consult/consult/consultList");		//??????????????? ??????
	                    menuAuths.add("/consult/consult/consultantList");		//???????????????
	                    menuAuths.add("/consult/consult/connList");			//????????????
	                    menuAuths.add("/consult/system/memberList");			//????????? ????????????
	                    menuAuths.add("/consult/system/detailMgmt");			//??????????????????	
	                    menuAuths.add("/consult/user/profile");			//???????????????
	                } else {
	                    //rMsg = messageSource.getMessage("login.fail.missmatchPassword", null, localeResolver.resolveLocale(request));
	                    logger.error("AuthProvider.authenticate() ????????? ??? ?????? ??????");
	                }

	                for (int i = 0; i < menuAuths.size(); i++) {

	                    platform_auth += menuAuths.get(i);
	                    if (i != (menuAuths.size() - 1)) {
	                        platform_auth += ",";
	                    }
	                }

	                result.setDetails(new CustomConsultDetails(
	                		consult_result.getMEMBER_CODE()
	                        , consult_result.getMEMBER_ID()
	                        , consult_result.getMEMBER_NAME()
	                        , platform_gubun
	                        , platform_auth
	                        , consult_result.getBRANCH_CODE()
	                        , consult_result.getBRANCH_NAME()
	                        , consult_result.getMEMBER_AUTHORITY()
	                        , consult_result.getMEMBER_AUTHORITY_NAME()
	                        , consult_result.getMEMBER_OTP()
	                ));

	                
	                session.setAttribute("MEMBER_AUTHORITY", MEMBER_AUTHORITY);
	                session.setAttribute("menuAuths", menuAuths);


	                /*  ************* ???????????? ?????? ??? *************   */

	                return result;

	            } catch (DataAccessException e) {
	                rMsg = messageSource.getMessage("error.database.connection", null, localeResolver.resolveLocale(request));
	                throw new BadCredentialsException(rMsg);
	            } catch (Exception ex) {
	                String exceptionMsg = ex.getMessage();
	                request.setAttribute("member_id", member_id);
	                request.setAttribute("exceptionMsg", exceptionMsg);

	                ex.printStackTrace();
	                throw new BadCredentialsException(rMsg);
	            }
	            
			}else {
				return authentication;
			}
            
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    public boolean IPCheck(String userIp) {

        // ????????? IP????????? ??????
        String IP_BEND_WIDTH = "172.16.";

        //?????? ??????
        Boolean isCheck = false;

        try {

            String ipAddr = "";
            if (userIp.equalsIgnoreCase("0:0:0:0:0:0:0:1")) {
                InetAddress inetAddr = InetAddress.getLocalHost();
                ipAddr = inetAddr.getHostAddress();
                logger.info("???????????????IP ??????::: " + ipAddr);
                logger.info("???????????????IP ??????::: " + ipAddr.substring(0, 7));

                String ipChk = ipAddr.substring(0, 7);
//			    String ipChk = "192.168";

                isCheck = IP_BEND_WIDTH.equals(ipChk);
            } else {
                logger.info("????????? ip??? ????????????." + userIp);
                isCheck = false;
            }


        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return isCheck;
    }

}