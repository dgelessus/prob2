/**
 * 
 */
package de.prob.worksheet.servlets.sync;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

import de.prob.worksheet.WorksheetObjectMapper;
import de.prob.worksheet.block.impl.DefaultBlock;
import de.prob.worksheet.document.IWorksheetData;

/**
 * @author Rene
 * 
 */
@Singleton
public class SetBlock extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6543934467917126456L;

	Logger logger = LoggerFactory.getLogger(SetBlock.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {
		doPost(req, resp);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {
		req.setCharacterEncoding("UTF-8");
		logParameters(req);
		resp.setCharacterEncoding("UTF-8");
		final String wsid = req.getParameter("worksheetSessionId");
		HashMap<String, Object> sessionAttributes = (HashMap<String, Object>) req
				.getSession().getAttribute(wsid);
		if (sessionAttributes == null)
			sessionAttributes = new HashMap<String, Object>();
		IWorksheetData doc = (IWorksheetData) sessionAttributes.get("document");

		if (req.getSession().isNew() || doc == null) {
			System.err
					.println("No worksheet Document is initialized (first a call to newDocument Servlet is needed)");
			resp.getWriter().write("Error: No document is initialized");
			if (req.getSession().isNew())
				req.getSession().invalidate();
			return;
		}

		final WorksheetObjectMapper mapper = new WorksheetObjectMapper();
		final String blockString = req.getParameter("block");
		final DefaultBlock block = mapper.readValue(blockString,
				DefaultBlock.class);
		logger.debug("EditorContent in Block:{}", block.getEditor()
				.getEditorContent());

		doc.setBlock(block);

		// TODO add result msg;

		return;

	}

	private void logParameters(HttpServletRequest req) {
		String[] params = { "worksheetSessionId", "block" };
		String msg = "{ ";
		for (int x = 0; x < params.length; x++) {
			if (x != 0)
				msg += " , ";
			msg += params[x] + " : " + req.getParameter(params[x]);
		}
		msg += " }";
		logger.debug(msg);
	}

}
