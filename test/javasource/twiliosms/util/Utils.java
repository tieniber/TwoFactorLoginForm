package twiliosms.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Utils {

	public static String urlEncode(String value) {
		if (value == null) {
			return "";
		}
		try {
			//See: http://www.w3.org/TR/html4/interact/forms.html#h-17.13.4.1
			//See: http://docs.oracle.com/javase/7/docs/api/java/net/URLEncoder.html
			//See: http://tools.ietf.org/html/rfc3986#section-2.3
			//URLEncoder.encode differts on 2 things, spaces are encoded with +, as described in form-encoding.
			//This is replaces by %20, since that is always save.
			//Besides that, URLEncoder 'forgets' to encode '*', which is a reserved character..... So also replace it.
			return URLEncoder.encode(value, "UTF-8").replace("+", "%20").replace("*", "%2A");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
