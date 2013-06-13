/**
 * 
 */
package de.prob.worksheet.servlets.sync;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;

import de.prob.worksheet.document.IWorksheetData;
import de.prob.worksheet.document.impl.WorksheetDocument;

/**
 * @author Rene
 * 
 */
@Singleton
public class SetDocument extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 838311906090202227L;

	Logger logger = LoggerFactory.getLogger(SetDocument.class);

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
		this.doPost(req, resp);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {
		logParameters(req);
		resp.setCharacterEncoding("UTF-8");
		// Get Session and needed Attributes
		final HttpSession session = req.getSession();
		final String wsid = req.getParameter("worksheetSessionID");

		final ObjectMapper jsonMapper = new ObjectMapper();

		final String docString = req.getParameter("document");
		IWorksheetData doc;
		doc = jsonMapper.readValue(docString, WorksheetDocument.class);

		session.setAttribute("WorksheetDocument" + wsid, doc);
		// TODO add result msg;

		return;

	}

	private void logParameters(HttpServletRequest req) {
		String[] params = { "worksheetSessionId", "document" };
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