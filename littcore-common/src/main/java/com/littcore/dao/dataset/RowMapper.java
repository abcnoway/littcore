package com.littcore.dao.dataset;

import java.util.Map;

/**
 * .
 * 
 * <pre><b>Description：</b>
 *    
 * </pre>
 * 
 * <pre><b>Changelog：</b>
 *    
 * </pre>
 * 
 * @author <a href="mailto:littcai@hotmail.com">Bob.cai</a>
 * @since 2012-8-3
 * @version 1.0
 */
public interface RowMapper<T, K, V> {
	
	T mapRow(Map<K, V> rowMap, int rowNum);
}
