package us.brown.crro.eduglass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

@SuppressWarnings("serial")
public class DesktopAppServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String session = req.getParameter("SESSION");
		
		if (session == null) {
			resp.setContentType("text/plain");
			resp.getWriter().println("Hello, world");
		} else {
			//Now we see if they are asking for index XOR image
			String action = req.getParameter("ACTION");
			MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
		    syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
		    String key = session + "-";
			if (action.equals("INDEX")) {
				//Again, we access the memcache to get the current index. The desktop app keeps track of the current index, if they are different, it updates.
				//All access to the memcache is using the session
				key = key + action;
				Integer value = (Integer) syncCache.get(key); // read from cache
			    if (value == null) {
			    	// The session expired.
			    	resp.setStatus(HttpServletResponse.SC_CONFLICT);
			    	return;
			    	//syncCache.put(key, value); // populate cache
			    }
			    resp.setContentType("text/plain");
			    resp.setStatus(HttpServletResponse.SC_OK);
			    resp.getWriter().println(value.intValue());
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
			//Now we see if they are asking for index XOR image
			String action = req.getParameter("ACTION");
			MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
		    syncCache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
		    String key = session + "-";
			if (action.equals("NOTES")) {
				//We are receiving the notes, we store them in the 
				BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
				StringBuilder out = new StringBuilder();
		        String line;
		        while ((line = reader.readLine()) != null) {
		            out.append(line + "\n");
		        }
		        String notes = out.toString();
		        reader.close();
		        key = key + action;
		        //We store the notes in the MemCache
		        syncCache.put(key, notes);
			} else if (action.equals("INDEX")) {
				//We are receiving a new index. We update it in the memcache
				int index = Integer.parseInt(req.getParameter("INDEX"));
				key = key + action;
				syncCache.put(key, index);
			} else if (action.equals("IMAGE")){
				//we are receiving an image from the server. 
				String equation = req.getParameter("EQUATION");
				byte[] bytes = IOUtils.toByteArray(req.getInputStream());
				Image image = ImagesServiceFactory.makeImage(bytes);
				key = key + equation;
				syncCache.put(key, image);
				return;
			} else {
				//We don't know what the hell happened
				return;
			}
		}
	}
	
	
}
