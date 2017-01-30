// This file was generated by Mendix Modeler.
//
// WARNING: Only the following code will be retained when actions are regenerated:
// - the import list
// - the code between BEGIN USER CODE and END USER CODE
// - the code between BEGIN EXTRA CODE and END EXTRA CODE
// Other code you write will be lost the next time you deploy the project.
// Special characters, e.g., é, ö, à, etc. are supported in comments.

package twofactorauth.actions;

import java.util.HashMap;
import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.externalinterface.connector.RequestHandler;
import com.mendix.logging.ILogNode;
import com.mendix.m2ee.api.IMxRuntimeRequest;
import com.mendix.m2ee.api.IMxRuntimeResponse;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.ISession;
import com.mendix.systemwideinterfaces.core.IUser;
import com.mendix.webui.CustomJavaAction;

/**
 * Exposes a request handler at /login that takes two POST parameters:
 *  - user
 *  - password
 * 
 * If the credentials are valid, the system checks to see if two-factor auth (TFA) is enabled. If enabled, a token is sent to the user and a 202 response is returned to the browser. If disabled, the user is logged in and a 200 with cookies are returned to the browser.
 */
public class StartTwoFactorLoginHandler extends CustomJavaAction<java.lang.Boolean>
{
	private java.lang.String SendTFATokenMicroflow;
	private java.lang.String CheckTFAActiveMicroflow;
	private java.lang.String CheckTokenMicroflow;
	private java.lang.String ResetTokenMicroflow;

	public StartTwoFactorLoginHandler(IContext context, java.lang.String SendTFATokenMicroflow, java.lang.String CheckTFAActiveMicroflow, java.lang.String CheckTokenMicroflow, java.lang.String ResetTokenMicroflow)
	{
		super(context);
		this.SendTFATokenMicroflow = SendTFATokenMicroflow;
		this.CheckTFAActiveMicroflow = CheckTFAActiveMicroflow;
		this.CheckTokenMicroflow = CheckTokenMicroflow;
		this.ResetTokenMicroflow = ResetTokenMicroflow;
	}

	@Override
	public java.lang.Boolean executeAction() throws Exception
	{
		// BEGIN USER CODE
		TWOFACTORLOGIN.info("Starting up Login or Two-Factor Token Handler...");
		Core.addRequestHandler("login/", new TokenRequestHandler());
		TWOFACTORLOGIN.info("Starting up Login or Two-Factor Token Handler.....DONE");
		return true;		
		// END USER CODE
	}

	/**
	 * Returns a string representation of this action
	 */
	@Override
	public java.lang.String toString()
	{
		return "StartTwoFactorLoginHandler";
	}

	// BEGIN EXTRA CODE
	private static final String XAS_ID = "XASID";
	protected static ILogNode TWOFACTORLOGIN = Core.getLogger("TwoFactorLogin");
	
	class TokenRequestHandler extends RequestHandler{
		
		@Override
		public void processRequest(IMxRuntimeRequest request,
				IMxRuntimeResponse response, String arg2) throws Exception 
		{
			if ("POST".equals(request.getHttpServletRequest().getMethod())) {	
				try{
					ISession oldSession = this.getSessionFromRequest(request);
					String user = request.getParameter("user");
					String password = request.getParameter("password");
					String token = request.getParameter("token");
					IContext sysContext = Core.createSystemContext();
					
					if(user != null && password != null && token != null){
						this.handleThreeParameterRequest(sysContext, user, password, token, response, oldSession);
					} else if (user != null && password != null) {
						this.handleTwoParameterRequest(sysContext, user, password, response, oldSession);
					} else {
						response.setStatus(400);
						TWOFACTORLOGIN.info("Bad request, two-factor token request requires at least a username and password");
					}				
				}
				catch (Exception e){
					response.setStatus(500);
					TWOFACTORLOGIN.info(e.getMessage());
				}
			} else {
				response.setStatus(405);
			}
		}
		
		private void setCookies(IMxRuntimeResponse response, ISession session) {
			response.addCookie(XAS_SESSION_ID, session.getId().toString(),  "/", "", -1, true);
			response.addCookie(XAS_ID, "0."+Core.getXASId(),"/", "", -1, true);			 
		}
		
		private void handleTwoParameterRequest(IContext sysContext, String user, String password, IMxRuntimeResponse response, ISession oldSession) throws Exception  {	
			IUser userObj = Core.getUser(sysContext, user);

			if (userObj != null) {
				boolean validPassword = Core.authenticate(sysContext, userObj, password);
			
				if (!userObj.isActive()) {
					response.setStatus(401);
					TWOFACTORLOGIN.info(user + " tried to log in, but is inactive.");
				}
				else if (userObj.isBlocked()) {
					response.setStatus(401);
					TWOFACTORLOGIN.info(user + " is currently blocked due to failed login attempts");
				} else if (!validPassword) {
					logFailedLogin(sysContext, userObj);
					response.setStatus(401);
					TWOFACTORLOGIN.info(user + " failed to authenticate");		
				} else {
					//Check if two-factor auth is enabled
					HashMap<String, Object> params = new HashMap<String, Object>();
					params.put("user", user);
					boolean twoFactor = Core.execute(sysContext, CheckTFAActiveMicroflow, true, params);
					
					if (twoFactor) {
						//two-factor enabled, need to send the user a token
						HashMap<String, Object> checkParams = new HashMap<String, Object>();
						checkParams.put("user", user);
						boolean success = Core.execute(sysContext, SendTFATokenMicroflow, true, checkParams);
						
						if (success) {
							//token was sent
							//success code 202 means "accepted but not done processing" 
							TWOFACTORLOGIN.info(user + " uses two-factor authentication, a token was delivered to the user.");									
							response.setStatus(202);
						} else {
							TWOFACTORLOGIN.error("Unable to send TFA Token for user: " + user + ". The microflow " + SendTFATokenMicroflow + " returned false.");
							response.setStatus(503);
						}
					} else {
						//two-factor is off, good to go, log in the user
						TWOFACTORLOGIN.info("UserName: " + user + " does not have two-factor enabled");								
						TWOFACTORLOGIN.info("UserName: " + user + " is attempting one-factor login");
						performLogin(user, response, oldSession, userObj);
					}
				}
			} else {
				response.setStatus(401);
				TWOFACTORLOGIN.info(user + " failed to authenticate");	
			}
		}

		private void handleThreeParameterRequest(IContext sysContext, String user, String password, String token, IMxRuntimeResponse response, ISession oldSession) throws Exception {			
			IUser userObj = Core.getUser(sysContext, user);
			
			if (userObj != null) {
				boolean validPassword = Core.authenticate(sysContext, userObj, password);
						
				if (!userObj.isActive()) {
					response.setStatus(401);
					TWOFACTORLOGIN.info(user + " tried to log in, but is inactive.");
				} else if (userObj.isBlocked()) {
					response.setStatus(401);
					TWOFACTORLOGIN.info(user + " is currently blocked due to previous failed login attempts");
				} else if (!validPassword) {
					logFailedLogin(sysContext, userObj);
					response.setStatus(401);
					TWOFACTORLOGIN.info(user + " failed to authenticate");		
				} else {
					//verify token
					HashMap<String, Object> params = new HashMap<String, Object>();
					params.put("user", user);
					params.put("token", token);
					boolean validToken = Core.execute(sysContext, CheckTokenMicroflow, true, params);
					
					if (validToken) {
						//remove the token
						HashMap<String, Object> removeTokenParams = new HashMap<String, Object>();
						removeTokenParams.put("user", user);
						Core.execute(sysContext, ResetTokenMicroflow, true, removeTokenParams);
						
						//now actually log in
						TWOFACTORLOGIN.info("UserName: " + user + " is attempting two-factor login");
						performLogin(user, response, oldSession, userObj);
					} else {
						response.setStatus(401);
						TWOFACTORLOGIN.info(user + " provided an invalid token");
					}
				}
			} else {
				response.setStatus(401);
				TWOFACTORLOGIN.info(user + " failed to authenticate");
			}
		}
		
		private void performLogin(String user, IMxRuntimeResponse response, ISession oldSession, IUser userObj)
				throws CoreException {
			String oldSessionId = "";
			if (oldSession != null) {
				oldSessionId = oldSession.getId().toString();
			}
			
			ISession newSession = Core.initializeSession(userObj, oldSessionId);
			
			if (oldSession != null) {
				oldSession.destroy();
			}
			
			TWOFACTORLOGIN.info(user + " has been successfully logged into the application");

			response.setStatus(IMxRuntimeResponse.OK);
			setCookies(response, newSession);
		}		
		
		private void logFailedLogin(IContext sysContext, IUser userObj) {
			IMendixObject userMxObj = userObj.getMendixObject();
			system.proxies.User userEntity = system.proxies.User.initialize(sysContext, userMxObj);
			int newFailedLogins = userEntity.getFailedLogins() + 1;
			TWOFACTORLOGIN.info(userObj.getName() + " login failed. This is failed attempt: " + newFailedLogins);			

			try {
				userEntity.setFailedLogins(newFailedLogins);
				
				if(newFailedLogins >= 3) {
					userEntity.setBlocked(true);
					TWOFACTORLOGIN.info(userObj.getName() + " is blocked for 5 minutes");
				}				
				userEntity.commit(sysContext);
			} catch (Exception e) {
				TWOFACTORLOGIN.error("Unable to commit failed login attempts to user object: " + userObj.getName());
			}

		}
	}
	// END EXTRA CODE
}
