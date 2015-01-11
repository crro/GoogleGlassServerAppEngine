package us.brown.crro.eduglass;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.images.Image;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

@SuppressWarnings("serial")
public class GoogleGlassServerServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String session = req.getParameter("SESSION");
		
		if (session == null) {
			resp.setContentType("text/plain");
			resp.getWriter().println("Hello, David");
		} else if (session.equals("NEW")) {
			//we generate a new session and return it
			Random r = new Random();
			int Low = 1111;
			int High = 9999;
			int value = r.nextInt(High-Low) + Low;
			resp.setContentType("text/plain");
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.getWriter().println(Integer.toString(value));
		} else {
			//Now we see if they are asking for index XOR image
			String action = req.getParameter("ACTION");
			MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
		    syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
		    String key = session + "-";
			if (action.equals("IMAGE")) {
				//We access the memcache to get the requested image
				//All access to the memcache is using the session
				String equation = req.getParameter("EQUATION");
				key = key + equation;
				Image image = (Image) syncCache.get(key);
				if (image == null) {
			    	// The session expired.
			    	resp.setStatus(HttpServletResponse.SC_CONFLICT);
			    	resp.getWriter().println("Session Expired");
			    	return;
			    	//syncCache.put(key, value); // populate cache
			    }
				resp.setContentType("image/png");
				resp.setStatus(HttpServletResponse.SC_OK);
				resp.getOutputStream().write(image.getImageData());
			} else if (action.equals("INDEX")) {
				//Again, we access the memcache to get the current index.
				//All access to the memcache is using the session
				key = key + action;
				Integer value = (Integer) syncCache.get(key); // read from cache
			    if (value == null) {
			    	// The session expired.
			    	resp.setStatus(HttpServletResponse.SC_CONFLICT);
			    	resp.getWriter().println("Session Expired");
			    	return;
			    	//syncCache.put(key, value); // populate cache
			    }
			    resp.setContentType("text/plain");
			    resp.setStatus(HttpServletResponse.SC_OK);
			    resp.getWriter().println(value.intValue());
			} else if (action.equals("NOTES")) {
				//we send the notes to the Glass app.
				key = key + action;
				String notes = (String) syncCache.get(key);
				if (notes == null) {
			    	// The session expired.
			    	resp.setStatus(HttpServletResponse.SC_CONFLICT);
			    	return;
			    	//syncCache.put(key, value); // populate cache
			    }
				resp.setContentType("text/plain");
			    resp.setStatus(HttpServletResponse.SC_OK);
			    resp.getWriter().println(notes);
			} else {
				//We don't know what the hell happened
				return;
			}
		}
		
		
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String session = req.getParameter("SESSION");
		
		if (session == null) {
			resp.setContentType("text/plain");
			resp.getWriter().println("Hello, world");
		} else {
			String action = req.getParameter("ACTION");
			MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
		    syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
		    String key = session + "-";
			if (action.equals("START")) {
				//We store a status for the particular session, this command switches it to "on"
				key = key + "STATUS";
				syncCache.put(key, "ON");
			} else if (action.equals("END")) {
				//We store a status for the particular session, this command switches it to "off"
				key = key + "STATUS";
				syncCache.put(key, "OFF");
			} else if (action.equals("NEXT")) {
				//We retrieve the index and the add one to it
				key = key + "INDEX";
				Integer value = (Integer) syncCache.get(key); // read from cache
			    if (value == null) {
			    	// The session expired.
			    	resp.setStatus(HttpServletResponse.SC_CONFLICT);
			    	return;
			    	//syncCache.put(key, value); // populate cache
			    }
			    syncCache.put(key, new Integer(value.intValue() + 1));
			} else if (action.equals("PREVIOUS")) {
				//We retrieve the index and subtract one from it
				key = key + "INDEX";
				Integer value = (Integer) syncCache.get(key); // read from cache
			    if (value == null) {
			    	// The session expired.
			    	resp.setStatus(HttpServletResponse.SC_CONFLICT);
			    	return;
			    	//syncCache.put(key, value); // populate cache
			    }
			    syncCache.put(key, new Integer(value.intValue() - 1));
			}
			
		}
		
	}
}
