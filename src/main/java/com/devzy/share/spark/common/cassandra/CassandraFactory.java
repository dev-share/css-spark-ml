package com.glocalme.share.spark.common.cassandra;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.Mapper.Option;
import com.datastax.driver.mapping.MappingManager;

/**
 * @decription Cassandra数据服务封装
 * @author yi.zhang
 * @time 2017年6月2日 下午2:48:49
 * @since 1.0
 * @jdk 1.8
 */
@SuppressWarnings("all")
public class CassandraFactory {
	private static Logger logger = LogManager.getLogger();
	/**
	 * 过期时间(单位:秒)
	 */
	private static int EXPIRE_TIME = 15 * 24 * 60 * 60;

	protected Session session = null;
	
	protected MappingManager mapping = null;
	
	private String servers;
	private String keyspace;
	private String username;
	private String password;

	public String getServers() {
		return servers;
	}

	public void setServers(String servers) {
		this.servers = servers;
	}

	public String getKeyspace() {
		return keyspace;
	}

	public void setKeyspace(String keyspace) {
		this.keyspace = keyspace;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @decription 初始化配置
	 * @author yi.zhang
	 * @time 2017年6月2日 下午2:15:57
	 */
	public void init(String servers,String keyspace,String username,String password) {
		try {
			// socket 链接配置
			SocketOptions socket = new SocketOptions();
			socket.setKeepAlive(true);
			socket.setReceiveBufferSize(1024* 1024);
			socket.setSendBufferSize(1024* 1024);
			socket.setConnectTimeoutMillis(5 * 1000);
			socket.setReadTimeoutMillis(1000);
			//设置连接池
			PoolingOptions pool = new PoolingOptions();
			// pool.setMaxRequestsPerConnection(HostDistance.LOCAL, 32);
			// pool.setMaxRequestsPerConnection(HostDistance.REMOTE, 32);
			// pool.setCoreConnectionsPerHost(HostDistance.LOCAL, 2);
			// pool.setCoreConnectionsPerHost(HostDistance.REMOTE, 2);
			// pool.setMaxConnectionsPerHost(HostDistance.LOCAL, 4);
			// pool.setMaxConnectionsPerHost(HostDistance.REMOTE, 4);
			pool.setHeartbeatIntervalSeconds(60);
			pool.setIdleTimeoutSeconds(120);
			pool.setPoolTimeoutMillis(5 * 1000);
			List<InetSocketAddress> saddress = new ArrayList<InetSocketAddress>();
			if (servers != null && !"".equals(servers)) {
				for (String server : servers.split(",")) {
					String[] address = server.split(":");
					String ip = address[0];
					int port = 9042;
					if (address != null && address.length > 1) {
						port = Integer.valueOf(address[1]);
					}
					saddress.add(new InetSocketAddress(ip, port));
				}
			}
			InetSocketAddress[] addresses = new InetSocketAddress[saddress.size()];
			saddress.toArray(addresses);
			
			Builder builder = Cluster.builder();
	        builder.withSocketOptions(socket);
	        // 设置压缩方式
	        builder.withCompression(ProtocolOptions.Compression.LZ4);
	        // 负载策略
//	        DCAwareRoundRobinPolicy loadBalance = DCAwareRoundRobinPolicy.builder().withLocalDc("localDc").withUsedHostsPerRemoteDc(2).allowRemoteDCsForLocalConsistencyLevel().build();
//	        builder.withLoadBalancingPolicy(loadBalance);
	        // 重试策略
	        builder.withRetryPolicy(DefaultRetryPolicy.INSTANCE);
			builder.withPoolingOptions(pool);
			builder.addContactPointsWithPorts(addresses);
			builder.withCredentials(username, password);
			Cluster cluster = builder.build();
			if (keyspace != null && !"".equals(keyspace)) {
				session = cluster.connect(keyspace);
			} else {
				session = cluster.connect();
			}
			mapping = new MappingManager(session);
		} catch (Exception e) {
			logger.error("-----Cassandra Config init Error-----", e);
		}
	}

	/**
	 * @decription 保存数据
	 * @author yi.zhang
	 * @time 2017年6月2日 下午6:18:49
	 * @param obj
	 * @return
	 */
	public int save(Object obj) {
		try {
			if(session!=null){
				init(servers, keyspace, username, password);
			}
			Mapper mapper = mapping.mapper(obj.getClass());
			mapper.save(obj, Option.saveNullFields(true));
			return 1;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * @decription 更新数据
	 * @author yi.zhang
	 * @time 2017年6月2日 下午6:19:08
	 * @param obj
	 * @return
	 */
	public int update(Object obj) {
		try {
			if(session!=null){
				init(servers, keyspace, username, password);
			}
			Mapper mapper = mapping.mapper(obj.getClass());
			mapper.save(obj, Option.saveNullFields(false),Option.ttl(EXPIRE_TIME));
			return 1;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * @decription 删除数据
	 * @author yi.zhang
	 * @time 2017年6月2日 下午6:19:25
	 * @param obj
	 * @return
	 */
	public int delete(Object obj) {
		try {
			if(session!=null){
				init(servers, keyspace, username, password);
			}
			Mapper mapper = mapping.mapper(obj.getClass());
			mapper.delete(obj);
			return 1;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * @decription 数据操作(Insert|Update|Delete)
	 * @author yi.zhang
	 * @time 2017年6月2日 下午6:19:40
	 * @param cql
	 * @param params
	 * @return
	 */
	public int executeUpdate(String cql, Object... params) {
		try {
			if(session!=null){
				init(servers, keyspace, username, password);
			}
			ResultSet rs = session.execute(cql, params);
			return rs.getAvailableWithoutFetching();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * @decription 数据库查询(Select)
	 * @author yi.zhang
	 * @time 2017年6月2日 下午2:16:12
	 * @param cql
	 *            cql语句
	 * @param params
	 *            占位符参数
	 * @param clazz
	 *            映射对象
	 * @return
	 */
	public List<?> executeQuery(String cql, Class clazz, Object... params) {
		try {
			if(session!=null){
				init(servers, keyspace, username, password);
			}
			List<Object> list = new ArrayList<Object>();
			ResultSet rs = session.execute(cql, params);
			ColumnDefinitions rscd = rs.getColumnDefinitions();
			int count = rscd.size();
			Map<String, String> reflect = new HashMap<String, String>();
			for (int i = 0; i < count; i++) {
				String column = rscd.getName(i);
				String tcolumn = column.replaceAll("_", "");
				if (clazz == null) {
					reflect.put(column, column);
				} else {
					Field[] fields = clazz.getDeclaredFields();
					for (Field field : fields) {
						String tfield = field.getName();
						if (tcolumn.equalsIgnoreCase(tfield)) {
							reflect.put(column, tfield);
							break;
						}
					}
				}
			}
			for (Row row : rs.all()) {
				JSONObject obj = new JSONObject();
				for (String column : reflect.keySet()) {
					String key = reflect.get(column);
					Object value = row.getObject(column);
					obj.put(key, value);
				}
				Object object = obj;
				if (clazz != null) {
					object = JSON.parseObject(obj.toJSONString(), clazz);
				}
				list.add(object);
			}
			return list;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @decription 查询数据表字段名(key:字段名,value:字段类型名)
	 * @author yi.zhang
	 * @time 2017年6月30日 下午2:16:02
	 * @param table	表名
	 * @return
	 */
	public Map<String,String> queryColumns(String table){
		try {
			String sql = "select * from "+table;
			ResultSet rs = session.execute(sql);
			ColumnDefinitions rscd = rs.getColumnDefinitions();
			int count = rscd.size();
			Map<String,String> reflect = new HashMap<String,String>();
			for (int i = 0; i < count; i++) {
				String column = rscd.getName(i);
				String type = rscd.getType(i).getName().name().toLowerCase();
				reflect.put(column, type);
			}
			return reflect;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * @decription 查询数据库表名
	 * @author yi.zhang
	 * @time 2017年6月30日 下午2:16:02
	 * @param table	表名
	 * @return
	 */
	public List<String> queryTables(){
		try {
			List<String> tables = new ArrayList<String>();
			String useQuery = "describe tables";
			ResultSet rs = session.execute(useQuery);
			for (Row row : rs.all()) {
				String table = row.getString(1);
				tables.add(table);
			}
			return tables;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}