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
import java.util.Map;
import java.util.UUID;
import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.AuthenticationRuntimeException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.ISession;
import com.mendix.systemwideinterfaces.core.IUser;
import com.mendix.systemwideinterfaces.core.UserAction;
import com.mendix.systemwideinterfaces.core.UserActionListener;
import com.mendix.webui.CustomJavaAction;

/**
 * Overrides the standard Mendix login function with a control for users with two-factor auth (TFA) enabled. These users will be unable to log in using the standard Mendix login functions.
 */
public class StartDefaultLoginOverrideHandler extends CustomJavaAction<java.lang.Boolean>
{
	private java.lang.String CheckTFAActiveMicroflow;

	public StartDefaultLoginOverrideHandler(IContext context, java.lang.String CheckTFAActiveMicroflow)
	{
		super(context);
		this.CheckTFAActiveMicroflow = CheckTFAActiveMicroflow;
	}

	@Override
	public java.lang.Boolean executeAction() throws Exception
	{
		// BEGIN USER CODE
		CheckTFAActiveMicroflowStatic = this.CheckTFAActiveMicroflow;
		Core.addUserAction(CustomLoginAction.class);
		CustomActionListener listener = new CustomActionListener();
		listener.addReplaceEvent(CustomLoginAction.class.getName());
		Core.addListener(listener);
		
		return true;
		// END USER CODE
	}

	/**
	 * Returns a string representation of this action
	 */
	@Override
	public java.lang.String toString()
	{
		return "StartDefaultLoginOverrideHandler";
	}

	// BEGIN EXTRA CODE
	protected static java.lang.String CheckTFAActiveMicroflowStatic;
	
	public static class CustomLoginAction extends com.mendix.core.action.user.LoginAction {
		public CustomLoginAction( Map<String, ? extends Object> params) {
			super(Core.createSystemContext(), params);		
		}
	
		@Override
		public ISession executeAction() throws Exception {
			
			//Check if two-factor auth is enabled
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("user", this.getUserName());
			boolean twoFactor = Core.execute(Core.createSystemContext(), CheckTFAActiveMicroflowStatic, true, params);
			
			if (twoFactor) {
				return null;
			} else {
				//This executes the standard login functionality from the platform, if the login fails we get an authRuntimeException
				ISession newSession = super.executeAction();
				return newSession;
			}
			
		}
	}
	
	public static class CustomActionListener extends UserActionListener<com.mendix.core.action.user.LoginAction> {
		/**
		 * @param targetClass
		 */
		public CustomActionListener() {
			super(com.mendix.core.action.user.LoginAction.class);
		}
	
		/**
		 * This action decides if the custom login action should be executed, when the result is true
		 * the custom login action will be executed
		 */
		@Override
		public boolean check(com.mendix.core.action.user.LoginAction action) {
			if (action == null)
				throw new IllegalArgumentException("Action should not be null");
			return true;
		}
	}

	// END EXTRA CODE
}