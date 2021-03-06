package com.littcore.web.handler;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.TypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;
import org.springframework.web.util.WebUtils;

import com.littcore.common.BeanManager;
import com.littcore.exception.BusiCodeException;

/**
 * {@link org.springframework.web.servlet.HandlerExceptionResolver} implementation that allows for mapping exception
 * class names to view names, either for a set of given handlers or for all handlers in the DispatcherServlet.
 *
 * <p>Error views are analogous to error page JSPs, but can be used with any kind of exception including any checked
 * one, with fine-granular mappings for specific handlers.</p>
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 22.11.2003
 * @see org.springframework.web.servlet.DispatcherServlet
 */
public class SuffixMappingExceptionResolver extends AbstractHandlerExceptionResolver {

	/** The default name of the exception attribute: "exception". */
	public static final String DEFAULT_EXCEPTION_ATTRIBUTE = "exception";

	private Properties exceptionMappings;
	
	/**
	 * 后缀名映射.
	 * 通过匹配URL的后缀名，找到对应的errorView进行渲染
	 */
	private Properties suffixMappings;

	private String defaultErrorView;

	private Integer defaultStatusCode;

	/** 
	 * 状态编号.
	 * 可修改指定view返回的HTTP头中的状态编号.
	 *  
	 */
	private Map<String, Integer> statusCodes = new HashMap<String, Integer>();

	private String exceptionAttribute = DEFAULT_EXCEPTION_ATTRIBUTE;


	/**
	 * Set the mappings between exception class names and error view names.
	 * The exception class name can be a substring, with no wildcard support at present.
	 * A value of "ServletException" would match <code>javax.servlet.ServletException</code>
	 * and subclasses, for example.
	 * <p><b>NB:</b> Consider carefully how
	 * specific the pattern is, and whether to include package information (which isn't mandatory).
	 * For example, "Exception" will match nearly anything, and will probably hide other rules.
	 * "java.lang.Exception" would be correct if "Exception" was meant to define a rule for all
	 * checked exceptions. With more unusual exception names such as "BaseBusinessException"
	 * there's no need to use a FQN.
	 * @param mappings exception patterns (can also be fully qualified class names) as keys,
	 * and error view names as values
	 */
	public void setExceptionMappings(Properties mappings) {
		this.exceptionMappings = mappings;
	}

	/**
	 * Set the name of the default error view. This view will be returned if no specific mapping was found. <p>Default is
	 * none.
	 */
	public void setDefaultErrorView(String defaultErrorView) {
		this.defaultErrorView = defaultErrorView;
	}

	/**
	 * Set the HTTP status code that this exception resolver will apply for a given resolved error view. Keys are
	 * view names; values are status codes.
	 * <p>Note that this error code will only get applied in case of a top-level request. It will not be set for an include
	 * request, since the HTTP status cannot be modified from within an include.
	 * <p>If not specified, the default status code will be applied.
	 * @see #setDefaultStatusCode(int)
	 */
	public void setStatusCodes(Properties statusCodes) {
		for (Enumeration enumeration = statusCodes.propertyNames(); enumeration.hasMoreElements();) {
			String viewName = (String) enumeration.nextElement();
			Integer statusCode = new Integer(statusCodes.getProperty(viewName));
			this.statusCodes.put(viewName, statusCode);
		}
	}

	/**
	 * Set the default HTTP status code that this exception resolver will apply if it resolves an error view and if there
	 * is no status code mapping defined.
	 * <p>Note that this error code will only get applied in case of a top-level request. It will not be set for an
	 * include request, since the HTTP status cannot be modified from within an include.
	 * <p>If not specified, no status code will be applied, either leaving this to the controller or view, or keeping
	 * the servlet engine's default of 200 (OK).
	 * @param defaultStatusCode HTTP status code value, for example 500
	 * ({@link HttpServletResponse#SC_INTERNAL_SERVER_ERROR}) or 404 ({@link HttpServletResponse#SC_NOT_FOUND})
	 * @see #setStatusCodes(Properties)
	 */
	public void setDefaultStatusCode(int defaultStatusCode) {
		this.defaultStatusCode = defaultStatusCode;
	}

	/**
	 * Set the name of the model attribute as which the exception should be exposed. Default is "exception". <p>This can be
	 * either set to a different attribute name or to <code>null</code> for not exposing an exception attribute at all.
	 * @see #DEFAULT_EXCEPTION_ATTRIBUTE
	 */
	public void setExceptionAttribute(String exceptionAttribute) {
		this.exceptionAttribute = exceptionAttribute;
	}

	/**
	 * Actually resolve the given exception that got thrown during on handler execution, returning a ModelAndView that
	 * represents a specific error page if appropriate. <p>May be overridden in subclasses, in order to apply specific
	 * exception checks. Note that this template method will be invoked <i>after</i> checking whether this resolved applies
	 * ("mappedHandlers" etc), so an implementation may simply proceed with its actual exception handling.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or <code>null</code> if none chosen at the time of the exception (for example,
	 * if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding ModelAndView to forward to, or <code>null</code> for default processing
	 */
	@Override
	protected ModelAndView doResolveException(HttpServletRequest request,
			HttpServletResponse response,
			Object handler,
			Exception ex) {
		//如果是业务编码异常，
		if(ex instanceof BusiCodeException)
		{
		  logger.error(ex);
			BusiCodeException busiCodeException = (BusiCodeException)ex;
			//根据BusiCode获得国际化内容，再转换为
			Locale locale = com.littcore.web.util.WebUtils.getLocale(request);
			if(locale==null)
				locale = busiCodeException.getLocale();
			String message = BeanManager.getMessage(busiCodeException.getErrorCode(), busiCodeException.getParams(), locale);
			//将翻译后内容重新封装，同时返回errorCode方便客户端做细分逻辑
			ex = new BusiCodeException(message).setErrorCode(busiCodeException.getErrorCode());
		}
		else if(ex instanceof TypeMismatchException)
		{
      //根据BusiCode获得国际化内容，再转换为
      Locale locale = com.littcore.web.util.WebUtils.getLocale(request);
      String message = BeanManager.getMessage("error.typeMismatchException", null, locale);
		  
		  ex = new IllegalArgumentException(message);
		}
		// Expose ModelAndView for chosen error view.
		String viewName = determineViewName(ex, request);	
		if (viewName != null) {
			// Apply HTTP status code for error views, if specified.
			// Only apply it if we're processing a top-level request.
			Integer statusCode = determineStatusCode(request, viewName);
			if (statusCode != null) {
				applyStatusCodeIfPossible(request, response, statusCode);
			}   
			return getModelAndView(viewName, ex, request);
		}
		else {
			return null;
		}
	}

	/**
	 * Determine the view name for the given exception, searching the {@link #setExceptionMappings "exceptionMappings"},
	 * using the {@link #setDefaultErrorView "defaultErrorView"} as fallback.
	 * @param ex the exception that got thrown during handler execution
	 * @param request current HTTP request (useful for obtaining metadata)
	 * @return the resolved view name, or <code>null</code> if none found
	 */
	protected String determineViewName(Exception ex, HttpServletRequest request) {
		String viewName = null;
		//Check for specific ext mappings.
		if (this.suffixMappings != null) {
			viewName = findMatchingViewName(this.suffixMappings, request);
			if (viewName != null)
				return viewName;
		}
		
		// Check for specific exception mappings.
		if (this.exceptionMappings != null) {
			viewName = findMatchingViewName(this.exceptionMappings, ex);
		}
		// Return default error view else, if defined.
		if (viewName == null && this.defaultErrorView != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Resolving to default view '" + this.defaultErrorView + "' for exception of type [" +
						ex.getClass().getName() + "]");
			}
			viewName = this.defaultErrorView;
		}
		return viewName;
	}
	
	/**
	 * Find a matching view name in the given exception mappings.
	 * @param exceptionMappings mappings between exception class names and error view names
	 * @param ex the exception that got thrown during handler execution
	 * @return the view name, or <code>null</code> if none found
	 * @see #setExceptionMappings
	 */
	protected String findMatchingViewName(Properties suffixMappings, HttpServletRequest request) {
		String viewName = null;
		String requestURI = request.getRequestURI();
		String suffix = null;
		for (Enumeration names = suffixMappings.propertyNames(); names.hasMoreElements();) {
			suffix = (String) names.nextElement();			
			if (requestURI.endsWith("."+suffix)) {				
				viewName = suffixMappings.getProperty(suffix);
			}
		}
		if (viewName != null && logger.isDebugEnabled()) {
			logger.debug("Resolving to view '" + viewName + "' for suffix [" + suffix +
					"]");
		}
		return viewName;
	}

	/**
	 * Find a matching view name in the given exception mappings.
	 * @param exceptionMappings mappings between exception class names and error view names
	 * @param ex the exception that got thrown during handler execution
	 * @return the view name, or <code>null</code> if none found
	 * @see #setExceptionMappings
	 */
	protected String findMatchingViewName(Properties exceptionMappings, Exception ex) {
		String viewName = null;
		String dominantMapping = null;
		int deepest = Integer.MAX_VALUE;
		for (Enumeration names = exceptionMappings.propertyNames(); names.hasMoreElements();) {
			String exceptionMapping = (String) names.nextElement();
			int depth = getDepth(exceptionMapping, ex);
			if (depth >= 0 && depth < deepest) {
				deepest = depth;
				dominantMapping = exceptionMapping;
				viewName = exceptionMappings.getProperty(exceptionMapping);
			}
		}
		if (viewName != null && logger.isDebugEnabled()) {
			logger.debug("Resolving to view '" + viewName + "' for exception of type [" + ex.getClass().getName() +
					"], based on exception mapping [" + dominantMapping + "]");
		}
		return viewName;
	}

	/**
	 * Return the depth to the superclass matching.
	 * <p>0 means ex matches exactly. Returns -1 if there's no match.
	 * Otherwise, returns depth. Lowest depth wins.
	 */
	protected int getDepth(String exceptionMapping, Exception ex) {
		return getDepth(exceptionMapping, ex.getClass(), 0);
	}

	private int getDepth(String exceptionMapping, Class exceptionClass, int depth) {
		if (exceptionClass.getName().contains(exceptionMapping)) {
			// Found it!
			return depth;
		}
		// If we've gone as far as we can go and haven't found it...
		if (exceptionClass.equals(Throwable.class)) {
			return -1;
		}
		return getDepth(exceptionMapping, exceptionClass.getSuperclass(), depth + 1);
	}

	/**
	 * Determine the HTTP status code to apply for the given error view.
	 * <p>The default implementation returns the status code for the given view name (specified through the
	 * {@link #setStatusCodes(Properties) statusCodes} property), or falls back to the
	 * {@link #setDefaultStatusCode defaultStatusCode} if there is no match.
	 * <p>Override this in a custom subclass to customize this behavior.
	 * @param request current HTTP request
	 * @param viewName the name of the error view
	 * @return the HTTP status code to use, or <code>null</code> for the servlet container's default
	 * (200 in case of a standard error view)
	 * @see #setDefaultStatusCode
	 * @see #applyStatusCodeIfPossible
	 */
	protected Integer determineStatusCode(HttpServletRequest request, String viewName) {
		if (this.statusCodes.containsKey(viewName)) {
			return this.statusCodes.get(viewName);
		}
		if(com.littcore.web.util.WebUtils.isAjaxRequest(request))
		{
			return HttpServletResponse.SC_BAD_REQUEST;
			
		}
		return this.defaultStatusCode;
	}

	/**
	 * Apply the specified HTTP status code to the given response, if possible (that is,
	 * if not executing within an include request).
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param statusCode the status code to apply
	 * @see #determineStatusCode
	 * @see #setDefaultStatusCode
	 * @see HttpServletResponse#setStatus
	 */
	protected void applyStatusCodeIfPossible(HttpServletRequest request, HttpServletResponse response, int statusCode) {
		if (!WebUtils.isIncludeRequest(request)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Applying HTTP status code " + statusCode);
			}
			response.setStatus(statusCode);
			request.setAttribute(WebUtils.ERROR_STATUS_CODE_ATTRIBUTE, statusCode);
		}
	}

	/**
	 * Return a ModelAndView for the given request, view name and exception.
	 * <p>The default implementation delegates to {@link #getModelAndView(String, Exception)}.
	 * @param viewName the name of the error view
	 * @param ex the exception that got thrown during handler execution
	 * @param request current HTTP request (useful for obtaining metadata)
	 * @return the ModelAndView instance
	 */
	protected ModelAndView getModelAndView(String viewName, Exception ex, HttpServletRequest request) {
		return getModelAndView(viewName, ex);
	}

	/**
	 * Return a ModelAndView for the given view name and exception.
	 * <p>The default implementation adds the specified exception attribute.
	 * Can be overridden in subclasses.
	 * @param viewName the name of the error view
	 * @param ex the exception that got thrown during handler execution
	 * @return the ModelAndView instance
	 * @see #setExceptionAttribute
	 */
	protected ModelAndView getModelAndView(String viewName, Exception ex) {
		ModelAndView mv = new ModelAndView(viewName);
		if (this.exceptionAttribute != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Exposing Exception as model attribute '" + this.exceptionAttribute + "'", ex);
			}
			//如果是业务编码类异常，额外返回错误代码，方便客户端做细化的业务处理
			if(ex instanceof BusiCodeException)
			{
			  BusiCodeException busiCodeException = (BusiCodeException)ex;
			  mv.addObject("errorCode", busiCodeException.getErrorCode());
			}
			mv.addObject("className", ex.getClass().getName());
			mv.addObject(this.exceptionAttribute, ex.getMessage());
		}
		return mv;
	}

	/**
	 * @param suffixMappings the suffixMappings to set.
	 */
	public void setSuffixMappings(Properties suffixMappings)
	{
		this.suffixMappings = suffixMappings;
	}

}
