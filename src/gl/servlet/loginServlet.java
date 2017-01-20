package gl.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sun.misc.BASE64Decoder;


//@WebServlet("/login")
public class loginServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final String accountMenuError="[{id:\"account-error\",text:\"error\",leaf:true}]";
	//private static final String SQLDRIVERNAME="com.microsoft.sqlserver.jdbc.SQLServerDriver";
	//private static final String SQLURLHead="jdbc:sqlserver://";
	private static final String MYSQLDRIVERNAME="com.mysql.jdbc.Driver";  
	private static final String MYSQLURLHead="jdbc:mysql://";
	private static final String MYSQLURLEnding="?useUnicode=false&characterEncoding=";//utf-8	
	private static final String URLTimeout="&timeout=";
	//private static final int reconnNum=5; // mysql网络异常重连次数 
	
	private @Resource(name= "mysqlip" ) String mysqlip;
	private @Resource(name= "mysqlport" ) String mysqlport;
	private @Resource(name= "ConnLanCode" ) String ConnLanCode;
	private @Resource(name= "conntimeout" ) int conntimeout; 
	private @Resource(name= "dbname" ) String dbname;
	private @Resource(name= "user_privi" ) String user_privi;
	private @Resource(name= "user_privi_passwd" ) String user_privi_passwd; 
	private @Resource(name= "sleeptime" ) int sleeptime; 
    private @Resource(name= "reconnNum" ) int reconnNum; 
    private BASE64Decoder decoder = new BASE64Decoder();
    
    private void dojob(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	//System.out.println("in dojob");
    	response.setContentType("text/json;charset=gbk");
    	PrintWriter out = response.getWriter();
    	Map<String,String> cookiesMap=analyseCookies(request);
		Connection conn = retryGetConn();
    	String userid = checkuser(request,cookiesMap,conn);
    	try{
    		if(userid == null){
        		out.print("1"); 
        		out.flush();
        		return;
        	}
        	
        	String key=request.getParameter("skey");
        	System.out.println("key:"+key);
        	
        	//check user
        	if( key != null && "5tgb8ik,".equals(key)){
        		String outstr = userlogin(userid,cookiesMap,conn);
        		if(outstr != null){
        			out.print(outstr);
        			out.flush();
        		}
    			return;
    		}	
    		
    		//index.html create menu
    		if( key != null && "5tgb7ujm".equals(key)){
    			String outstr = createMenu(userid,cookiesMap,conn);
    			if(outstr != null){
        			out.print(outstr);
        			out.flush();
        		}
    			return;
    		}
    		
    		if(key != null && "4rfv6yhn".equals(key)){
    			String outstr = getVersion(userid,cookiesMap,conn);
    			if(outstr != null){
        			out.print(outstr);
        			out.flush();
        		}
    			return;
    		}
    		
    		if(key != null && "4rfv5tgb".equals(key)){
    			String outstr = getGameList(userid,cookiesMap,conn);
    			if(outstr != null){
        			out.print(outstr);
        			out.flush();
        		}
    			return;
    		}
    		
    		if(key != null && "4rfv3edc".equals(key)){
    			String outstr = getAdvtype(userid,cookiesMap,conn);
    			if(outstr != null){
        			out.print(outstr);
        			out.flush();
        		}
    			return;
    		}
    		
    		if(key != null && "3edc5tgb".equals(key)){
    			String outstr = getCh(userid,cookiesMap,conn);
    			if(outstr != null){
        			out.print(outstr);
        			out.flush();
        		}
    			return;
    		}
    		
    		if(key != null && "3edc6yhn".equals(key)){
    			String outstr = getSCh(userid,cookiesMap,conn);
    			if(outstr != null){
        			out.print(outstr);
        			out.flush();
        		}
    			return;
    		}
    		
    		if(key != null && "3edc7ujm".equals(key)){
    			String outstr = getData(userid,cookiesMap,conn);
    			if(outstr != null){
        			out.print(outstr);
        			out.flush();
        		}
    			return;
    		}
    		
    	}finally{
    		if(conn !=null){
				try {
					conn.close();
				} catch (Exception e) {
					System.out.println("error loginServlet createMenu finally conn.close() exception:"+e.getMessage());
				}
				conn = null;
			}
			
			if(out != null){
				out.close();
			}
    	}		
    }
    
    private String getDefaultGame(Connection conn,String userid){
    	String gamename = null;
    	ResultSet gameRs = retrySelect(conn,"select g.gid,g.gamename from userid_gid u,game g where u.gid=g.gid and u.userid="+userid+" order by u.gid limit 1;");
		try {
			if(gameRs.next()){
				gamename = gameRs.getString("gamename");
				String gameid = gameRs.getString("gid");
				System.out.println("gamename do not set,get defalut gamename:"+gamename+",gid:"+gameid);
			}
		} catch (SQLException e) {
			System.out.println("error! getCh get default game false:"+e.getMessage());
		}finally{
			if(gameRs != null){
				try {
					gameRs.close();
				} catch (Exception e) {					
					System.out.println("error loginServlet getCh finally gameRs.close() exception:"+e.getMessage());
				}
				gameRs = null;
			}
		}	
		return gamename;
    }
    
    private String getDefaultCh(Connection conn,String gamename){
    	String ch = null;
    	ResultSet rs = retrySelect(conn,"select c.chid,c.chname from gid_chid gc,game g,ch c where c.chid=gc.chid and gc.gid=g.gid and g.gamename='"+gamename+"' order by c.chid limit 1;");
		try {
			if(rs.next()){
				ch = rs.getString("chname");
				String chid = rs.getString("chid");
				System.out.println("chname do not set,get defalut chname:"+ch+",chid:"+chid);
			}
		} catch (SQLException e) {
			System.out.println("error! getDefaultCh get default chname false:"+e.getMessage());
		}finally{
			if(rs != null){
				try {
					rs.close();
				} catch (Exception e) {					
					System.out.println("error loginServlet getgetDefaultChCh finally rs.close() exception:"+e.getMessage());
				}
				rs = null;
			}
		}	
		return ch;
    }
    
    private String getData(String userid,Map<String,String> cookiesMap,Connection conn){
    	String gamename = cookiesMap.get("gn");
    	String stime = cookiesMap.get("st");
    	String etime = cookiesMap.get("et");
		//get default game,if not set game
		if(gamename == null || "cfgnull".equals(gamename)){
			gamename=getDefaultGame(conn,userid);
		}
		System.out.println("getData game:"+gamename);
		if(gamename == null){
			System.out.println("error! getData userid:"+userid+",get gamename false.");			
			return null;
		}
		
		String chname = cookiesMap.get("chn");
		System.out.println("init chname:"+chname);
		chname = decode(chname);	
		System.out.println("chname:"+chname);
		
		
		if(chname == null || "cfgnull".equals(chname)){
			chname = getDefaultCh(conn,gamename);
		}
		if(chname == null){
			System.out.println("error! getData userid:"+userid+",gamename:"+gamename+",get chname false.");			
			return null;
		}
		
		System.out.println("getData paramter:gname:"+gamename+",chname:"+chname+",st:"+stime+",et:"+etime);
		ResultSet rs = null;
		//String outstr = "{\"d\":[";
		String outstr = "]}";
		String sql = "select d.* from rt_data d,game g,ch c  where g.gamename='"+gamename+"' and c.chname='"+chname+"' and d.gid=g.gid and d.chid=c.chid order by d.datatime desc limit 8;";
		int init = 0;
		if("所有渠道".equals(chname)){
			sql = "select d.* from rt_data d,game g  where g.gamename='"+gamename+"' and d.chid=-1 and d.gid=g.gid order by d.datatime desc limit 8;";
		}
		
		System.out.println("getdata sql:"+sql);
		DecimalFormat df = new DecimalFormat("00");
		try{
			rs = retrySelect(conn,sql);
			while(rs.next()){
				if(init == 0){
					init = 1;
				}else{
					outstr=","+outstr;
				}
				int sh = rs.getInt("sh");//
				int dc = rs.getInt("dc");//
				String crate = (sh <=0 ? "0":df.format(((float)dc/sh)*100));//
				
				int acadv = rs.getInt("acadv");//
				int actotal = rs.getInt("actotal");
				int accam = Math.max(0,actotal-acadv);//
				String acrate = (actotal <= 0 ? "0":df.format(((float)acadv/actotal)*100));//				
				
				int paynum = rs.getInt("paynum");
				int nwnum = rs.getInt("nwnum");
				//int advnum = rs.getInt("advnum");
				int charge = rs.getInt("charge");//
				int nwcharge = rs.getInt("nwcharge");
				int advconsume = rs.getInt("advconsume");
				
				int dauadv = rs.getInt("dauadv");//
				int dautotal = rs.getInt("dautotal");//
				int daucam = dautotal-dauadv;//
				
				int wauadv = rs.getInt("wauadv");//
				int wautotal = rs.getInt("wautotal");//
				int waucam = wautotal-wauadv;//
				
				int mauadv = rs.getInt("mauadv");//
				int mautotal = rs.getInt("mautotal");//
				int maucam = mautotal-mauadv;//							
				
				int roi = (charge <=0 ?0:advconsume/charge);//
				int ltv = (nwnum <= 0 ? 0 : nwcharge/nwnum);//
				int arpu = (dautotal<= 0 ? 0 : charge/dautotal);//
				int arppu = (paynum<=0 ? 0 : charge/paynum);//
				int nwpayrate = (nwnum<=0 ? 0 : paynum/nwnum);//新增付费率
				
				int lc1adv = rs.getInt("lc1adv");//
				int lc1total = rs.getInt("lc1total");//
				int lc1cam = lc1total-lc1adv;//								
				
				outstr = "{\"date\":\""+rs.getString("datatime").split("\\.")[0].split(" ")[1]+"\",\"sh\":"+sh+",\"dc\":"+dc+",\"crate\":"+crate+
						",\"acadv\":"+acadv+",\"accam\":"+accam+",\"acrate\":"+acrate+
						",\"roi\":"+roi+",\"ltv\":"+ltv+",\"arpu\":"+arpu+",\"arppu\":"+arppu+",\"charge\":"+charge+",\"nwpayrate\":"+nwpayrate+
						",\"dautotal\":"+dautotal+",\"daucam\":"+daucam+",\"dauadv\":"+dauadv+
						",\"wautotal\":"+wautotal+",\"waucam\":"+waucam+",\"wauadv\":"+wauadv+
						",\"mautotal\":"+mautotal+",\"maucam\":"+maucam+",\"mauadv\":"+mauadv+
						",\"lc1total\":"+lc1total+",\"lc1cam\":"+lc1cam+",\"lc1adv\":"+lc1adv+"}"+outstr;
			}
			
			if(init == 0 || !stime.equals("2017-01-20")){
				outstr = "{\"date\":\"0\",\"sh\":0,\"dc\":0,\"crate\":0,"
						+ "\"acadv\":0,\"accam\":0,\"acrate\":0,"
						+ "\"roi\":0,\"ltv\":0,\"arpu\":0,\"arppu\":0,\"charge\":0,\"nwpayrate\":0,"
						+ "\"dautotal\":0,\"daucam\":0,\"dauadv\":0,"
						+ "\"wautotal\":0,\"waucam\":0,\"wauadv\":0,"
						+ "\"mautotal\":0,\"maucam\":0,\"mauadv\":0,"
						+ "\"lc1total\":0,\"lc1cam\":0,\"lc1adv\":0}]}";
			}

			outstr ="{\"d\":["+outstr;
			System.out.println("data:"+outstr);
			return outstr;
		}catch(SQLException e){
			System.out.println("error! loginServlet getData exception:"+e.getMessage());
		}finally{
			if(rs != null){
				try {
					rs.close();
				} catch (Exception e) {					
					System.out.println("error loginServlet getData finally rs.close() exception:"+e.getMessage());
				}
				rs = null;
			}
		}
		return null;
    }
    
    private String getSCh(String userid,Map<String,String> cookiesMap,Connection conn){
    	//response.setContentType("text/json;charset=gbk");
//		PrintWriter out = response.getWriter();
//		Map<String,String> cookiesMap=analyseCookies(request);
//		Connection conn = retryGetConn();		
//		String userid = checkuser(request,cookiesMap,conn);
		
//		String username = null;
//		String password = null;  
//		String gamename = null;
//		Cookie[] cookies = request.getCookies();
//		int len = cookies.length;
//		for(int i=0;i<len;i++){
//			switch (cookies[i].getName()){
//				case "u":username=cookies[i].getValue();break;
//				case "k":password=cookies[i].getValue();break;
//				case "gn":gamename=cookies[i].getValue();break;
//			}
//		}
		
		//String userid = checkuser(username, password);
//		if( userid ==  null){
//			//login false
//			//System.out.println("error! getSCh userid:"+username+",pass:"+password+",check false.");
//			//out.print("["+accountMenuError+"]"); 
//			out.flush();
//			out.close();
//			return;
//		}
		
//    	Connection conn = retryGetConn();
//		if(conn == null){
//			System.out.print("error! getGameList retryGetConn conn is null");
//			//out.print("["+accountMenuError+"]"); 
//			out.flush();
//			out.close();
//			return;
//		}
		
		String gamename = cookiesMap.get("gn");
		System.out.println("sch game:"+gamename);
		if(gamename == null || "cfgnull".equals(gamename)){
			gamename=getDefaultGame(conn,userid);
		}
		
		if(gamename == null){
			System.out.println("error! getSCh userid:"+userid+",get gamename false.");
			//out.flush();
			//out.close();
			return null;
		}		
		
		ResultSet menuRs = null;
		String outstr = "{\"sch\":[";
		int init = 0;
		try{
			if(!"cfgnull".equals(gamename)){
				menuRs = retrySelect(conn,"select sc.schid,sc.schname from game g,gid_schid gc,sch sc  where g.gid=gc.gid and sc.schid=gc.schid and g.gamename='"+gamename+"';");
				while(menuRs.next()){
					if(init == 0){
						outstr+="{\"id\":-1,\"name\":\"所有小渠道\",\"boolean\":true},";
						init = 1;
					}else{
						outstr +=",";
						init++;
					}
					outstr+="{\"id\":"+menuRs.getString("schid")+",\"name\":\""+menuRs.getString("schname")+"\",\"boolean\":false}";					
				}
				outstr+="]}";
				System.out.println("schlist:"+outstr);
				return outstr;
				//out.print(outstr);
				//out.flush();
			}			
		}catch(SQLException e){
			System.out.println("error! loginServlet getSCh exception:"+e.getMessage());
		}finally{
			if(menuRs != null){
				try {
					menuRs.close();
				} catch (Exception e) {					
					System.out.println("error loginServlet getSCh finally priviRs.close() exception:"+e.getMessage());
				}
				menuRs = null;
			}
			
//			if(conn !=null){
//				try {
//					conn.close();
//				} catch (Exception e) {
//					System.out.println("error loginServlet getSCh finally conn.close() exception:"+e.getMessage());
//				}
//				conn = null;
//			}
//			
//			if(out != null){
//				out.close();
//			}
		}
		return null;
    }
    
    private String getCh(String userid,Map<String,String> cookiesMap,Connection conn){
//    	response.setContentType("text/json;charset=gbk");
//		PrintWriter out = response.getWriter();		
//		Map<String,String> cookiesMap=analyseCookies(request);
//		Connection conn = retryGetConn();		
//		String userid = checkuser(request,cookiesMap,conn);
		
//    	String username = null;
//		String password = null;  
//		String gamename = null;
//		Cookie[] cookies = request.getCookies();
//		int len = cookies.length;
//		for(int i=0;i<len;i++){
//			switch (cookies[i].getName()){
//				case "u":username=cookies[i].getValue();break;
//				case "k":password=cookies[i].getValue();break;
//				case "gn":gamename=cookies[i].getValue();break;
//			}
//		}
			
		//String userid = checkuser(username, password);
//		if( userid ==  null){
//			//login false
//			//System.out.println("error! getCh userid:"+username+",pass:"+password+",check false.");
//			//out.print("["+accountMenuError+"]"); 
//			out.flush();
//			out.close();
//			return;
//		}
		
//    	Connection conn = retryGetConn();
//		if(conn == null){
//			System.out.print("error! getGameList retryGetConn conn is null");
//			//out.print("["+accountMenuError+"]"); 
//			out.flush();
//			out.close();
//			return;
//		}
		String gamename = cookiesMap.get("gn");
		System.out.println("ch game:"+gamename);
		
		//get default game,if not set game
		//String gameid = null;
		if(gamename == null || "cfgnull".equals(gamename)){
			gamename=getDefaultGame(conn,userid);
		}
		
		ResultSet rs = null;
		String sql = null;
		String outstr = "{\"ch\":[";
		int init = 0;
//		if(gameid != null){
//			sql = "select c.chid,c.chname from game gid_chid gc,ch c  where c.chid=gc.chid and gc.gid='"+gameid+"';";
//		}else{
//			sql = "select c.chid,c.chname from game g,gid_chid gc,ch c  where g.gid=gc.gid and c.chid=gc.chid and g.gamename='"+gamename+"';";
//		}
		
		sql = "select c.chid,c.chname,c.bool from game g,gid_chid gc,ch c  where g.gid=gc.gid and (c.chid=gc.chid or c.chid =-1) and g.gamename='"+gamename+"';";
		try{			
			if(gamename != null && !"cfgnull".equals(gamename)){
				rs = retrySelect(conn,sql);
				while(rs.next()){
					if(init == 0){
						//outstr+="{\"id\":-1,\"name\":\"所有渠道\",\"boolean\":true},";
						init = 1;
					}else{
						outstr +=",";
						init++;
					}
					outstr+="{\"id\":"+rs.getString("chid")+",\"name\":\""+rs.getString("chname")+"\",\"boolean\":"+rs.getString("bool")+"}";					
				}
				outstr+="]}";
				System.out.println("chlist:"+outstr);
				return outstr;
				//out.print(outstr);
				//out.flush();
			}			
		}catch(SQLException e){
			System.out.println("error! loginServlet getCh exception:"+e.getMessage());
		}finally{
			if(rs != null){
				try {
					rs.close();
				} catch (Exception e) {					
					System.out.println("error loginServlet getCh finally priviRs.close() exception:"+e.getMessage());
				}
				rs = null;
			}
			
//			if(conn !=null){
//				try {
//					conn.close();
//				} catch (Exception e) {
//					System.out.println("error loginServlet getCh finally conn.close() exception:"+e.getMessage());
//				}
//				conn = null;
//			}
//			
//			if(out != null){
//				out.close();
//			}
		}
		return null;
    }
    
    private String getAdvtype(String userid,Map<String,String> cookiesMap,Connection conn) throws ServletException, IOException{
    	//response.setContentType("text/json;charset=gbk");
		//PrintWriter out = response.getWriter();
		//Map<String,String> cookiesMap=analyseCookies(request);
		//Connection conn = retryGetConn();		
		//String userid = checkuser(request,cookiesMap,conn);
		
    	//String username=request.getParameter("userid");
		//String password=request.getParameter("password");  //passwdEncode
		//String ip=getRemortIP(request);
		//String userid = checkuser(username, password);
//		if( userid ==  null){
//			//login false
//			//System.out.println("error! createMenu userid:"+username+",pass:"+password+",check false.");
//			//out.print("["+accountMenuError+"]"); 
//			out.flush();
//			out.close();
//			return;
//		}
		
//    	Connection conn = retryGetConn();
//		if(conn == null){
//			System.out.print("error! getGameList retryGetConn conn is null");
//			//out.print("["+accountMenuError+"]"); 
//			out.flush();
//			out.close();
//			return;
//		}
		
		ResultSet menuRs = retrySelect(conn,"select advtypeid,advtypename,bool from advtype;");
		String outstr = "{\"adv\":[";
		//String outstr = "[";
		int init = 0;
		try{														
			while(menuRs.next()){
				if(init == 0){
					init = 1;
				}else{
					outstr +=",";
					init++;
				}
				outstr+="{\"id\":"+menuRs.getString("advtypeid")+",\"name\":\""+menuRs.getString("advtypename")+"\",\"boolean\":"+menuRs.getString("bool")+"}";
				
			}
			outstr+="]}";
			//outstr+="],\"total\":"+init+"}";
			System.out.println("advlist:"+outstr);
			return outstr;
			//out.print(outstr);
			//out.flush();
		}catch(SQLException e){
			System.out.println("error! loginServlet getAdvtype exception:"+e.getMessage());
			return null;
		}finally{
			if(menuRs != null){
				try {
					menuRs.close();
				} catch (Exception e) {					
					System.out.println("error loginServlet getAdvtype finally priviRs.close() exception:"+e.getMessage());
				}
				menuRs = null;
			}
			
//			if(conn !=null){
//				try {
//					conn.close();
//				} catch (Exception e) {
//					System.out.println("error loginServlet getAdvtype finally conn.close() exception:"+e.getMessage());
//				}
//				conn = null;
//			}
//			
//			if(out != null){
//				out.close();
//			}
		}
    }
    
    private String getGameList(String userid,Map<String,String> cookiesMap,Connection conn){
    	//response.setContentType("text/json;charset=gbk");
		//PrintWriter out = response.getWriter();
		//Map<String,String> cookiesMap=analyseCookies(request);
		//Connection conn = retryGetConn();		
		//String userid = checkuser(request,cookiesMap,conn);
		
    	//String username=request.getParameter("userid");
		//String password=request.getParameter("password");  //passwdEncode
		//String ip=getRemortIP(request);
		//String userid = checkuser(username, password);
//		if( userid == null){
//			//login false
//			//System.out.println("error! createMenu userid:"+username+",pass:"+password+",check false.");
//			//out.print("["+accountMenuError+"]"); 
//			out.flush();
//			out.close();
//			return;
//		}
		
//    	Connection conn = retryGetConn();
//		if(conn == null){
//			System.out.print("error! getGameList retryGetConn conn is null");
//			//out.print("["+accountMenuError+"]"); 
//			out.flush();
//			out.close();
//			return;
//		}
		
		ResultSet menuRs = retrySelect(conn,"select g.gid,g.gamename from userid_gid u,game g where u.gid=g.gid and u.userid="+userid+";");
		//String outstr = "{\"g\":[";
		String outstr = "[";
		int init = 0;
		try{														
			while(menuRs.next()){
				if(init == 0){
					init = 1;
				}else{
					outstr +=",";
					init++;
				}
				outstr+="{\"id\":"+menuRs.getString("gid")+",\"name\":\""+menuRs.getString("gamename");
				
				if(init == 1){
					outstr+="\",\"boolean\":true}";
				}else{
					outstr+="\",\"boolean\":false}";
				}
			}
			outstr+="]";
			//outstr+="],\"total\":"+init+"}";
			System.out.println("gamelist:"+outstr);
			return outstr;
			//out.print(outstr);
			//out.flush();
		}catch(SQLException e){
			System.out.println("error! loginServlet getGameList exception:"+e.getMessage());
			return null;
		}finally{
			if(menuRs != null){
				try {
					menuRs.close();
				} catch (Exception e) {					
					System.out.println("error loginServlet getVersion finally priviRs.close() exception:"+e.getMessage());
				}
				menuRs = null;
			}
			
//			if(conn !=null){
//				try {
//					conn.close();
//				} catch (Exception e) {
//					System.out.println("error loginServlet getVersion finally conn.close() exception:"+e.getMessage());
//				}
//				conn = null;
//			}
//			
//			if(out != null){
//				out.close();
//			}
		}
    }
    
    private String getVersion(String userid,Map<String,String> cookiesMap,Connection conn){
    	//response.setContentType("text/json;charset=gbk");
		//PrintWriter out = response.getWriter();
		//Map<String,String> cookiesMap=analyseCookies(request);
		//Connection conn = retryGetConn();		
		//String userid = checkuser(request,cookiesMap,conn);
    	
//		if( userid ==  null){
//			//login false
//			//System.out.println("error! createMenu userid:"+username+",pass:"+password+",check false.");
//			//out.print("["+accountMenuError+"]"); 
//			out.flush();
//			out.close();
//			return;
//		}
		
		//Connection conn = retryGetConn();
//		if(conn == null){
//			System.out.print("error! getVersion retryGetConn conn is null");
//			out.flush();
//			out.close();
//			return;
//		}
		
		ResultSet menuRs = retrySelect(conn,"select vid,vname,bool from version");	
		//String outstr = "{\"v\":[";
		String outstr = "[";
		int init = 0;
		try{														
			while(menuRs.next()){
				if(init == 0){
					init = 1;
				}else{
					outstr +=",";
					init++;
				}
				outstr+="{\"id\":"+menuRs.getString("vid")+",\"name\":\""+menuRs.getString("vname")+"\",\"boolean\":"+menuRs.getBoolean("bool")+"}";
			}
			outstr+="]";
			//outstr+="],\"total\":"+init+"}";
			System.out.println("version:"+outstr);
			return outstr;
			//out.print(outstr);
			//out.flush();
		}catch(SQLException e){
			System.out.println("error! loginServlet getVersion exception:"+e.getMessage());
			return null;
		}finally{
			if(menuRs != null){
				try {
					menuRs.close();
				} catch (Exception e) {					
					System.out.println("error loginServlet getVersion finally priviRs.close() exception:"+e.getMessage());
				}
				menuRs = null;
			}
			
//			if(conn !=null){
//				try {
//					conn.close();
//				} catch (Exception e) {
//					System.out.println("error loginServlet getVersion finally conn.close() exception:"+e.getMessage());
//				}
//				conn = null;
//			}
//			
//			if(out != null){
//				out.close();
//			}
		}					
    }
    
    private String createMenu(String userid,Map<String,String> cookiesMap,Connection conn){
    	//response.setContentType("text/json;charset=gbk");
		//PrintWriter out = response.getWriter();
		//Map<String,String> cookiesMap=analyseCookies(request);
		//Connection conn = retryGetConn();		
		//String userid = checkuser(request,cookiesMap,conn);
//		if( userid == null){
//			//login false
//			//System.out.println("error! createMenu userid:"+username+",pass:"+password+",check false.");
//			out.print("["+accountMenuError+"]"); 
//			out.flush();
//			out.close();
//			return;
//		}
		
		//Connection conn = retryGetConn();
//		if(conn == null){
//			System.out.print("error! retryGetConn conn is null");
//			out.print("["+accountMenuError+"]"); 
//			out.flush();
//			out.close();
//			return;
//		}		
		
		//String sql = "select menuleafkey,menuleaftext,menuheadtext,menuheadkey,isleaf from privi p,menuleaf b,menuhead c where p.leafid=b.id and b.menuheadid=c.id and userid='"+username+"' order by menuheadid,b.id;";	
		String sql="select menuleafkey,menuleaftext,menuheadtext,menuheadkey,isleaf from privi p,menuleaf l,menuhead h where p.leafid=l.id and l.menuheadid=h.id and p.userid='"+userid+"' order by menuheadid,l.id;";
		String preMenuHead = null;
		String nowMenuHead="";
		String menuAllstr="[";
		int isLastHeadHaveLeaf = 0;
		ResultSet menuRs = retrySelect(conn,sql);	
		try{														
			while(menuRs.next()){
				//add head
				nowMenuHead=menuRs.getString("menuheadtext");
				System.out.println("menu:"+nowMenuHead);
				if(null == preMenuHead || !preMenuHead.equals(nowMenuHead)){
					if(preMenuHead != null && isLastHeadHaveLeaf == 1){
						menuAllstr=menuAllstr.replaceAll(",$","")+"]},";
					}
					isLastHeadHaveLeaf = 0;
					preMenuHead=nowMenuHead;
					if(menuRs.getInt("isleaf") == 1){
						menuAllstr+=printMenuLeaf(menuRs.getString("menuheadkey"),menuRs.getString("menuheadtext"));
						continue;
					}else{
						menuAllstr+=printMenuHead(preMenuHead);
					}
					
				}
				
				//leaf
				menuAllstr+=printMenuLeaf(menuRs.getString("menuleafkey"),menuRs.getString("menuleaftext"));
				isLastHeadHaveLeaf = 1;
			}
			if(preMenuHead != null && isLastHeadHaveLeaf == 1){
				menuAllstr=menuAllstr.replaceAll(",$","")+"]},";
			}
			menuAllstr=menuAllstr.replaceAll(",$","")+"]";
			
			System.out.println("create menu:"+menuAllstr);
			return menuAllstr;
			//out.print(menuAllstr); 
			//out.flush();			
		}catch(SQLException e){
			System.out.println("error! loginServlet createMenu exception:"+e.getMessage());
			return null;
		}finally{
			if(menuRs != null){
				try {
					menuRs.close();
				} catch (Exception e) {					
					System.out.println("error loginServlet createMenu finally priviRs.close() exception:"+e.getMessage());
				}
				menuRs = null;
			}
			
//			if(conn !=null){
//				try {
//					conn.close();
//				} catch (Exception e) {
//					System.out.println("error loginServlet createMenu finally conn.close() exception:"+e.getMessage());
//				}
//				conn = null;
//			}
			
//			if(out != null){
//				out.close();
//			}
		}
    }
	
    private String userlogin(String userid,Map<String,String> cookiesMap,Connection conn){
    	//response.setContentType("text/json;charset=gbk");
		//PrintWriter out = response.getWriter();
		//Map<String,String> cookiesMap=analyseCookies(request);
		//Connection conn = retryGetConn();
		//if(conn != null){
//			try{			
//				String userid = checkuser(request,cookiesMap,conn);
//				if( userid != null){
//					String gsinfo = getUserGs(conn,userid);
//					if(gsinfo != null){
//						out.print(gsinfo);
//						out.flush();
//						out.close();
//						return;
//					}	
//				}
//			}finally{
//				if(conn != null){
//					try {
//						conn.close();
//					} catch (Exception e) {					
//						System.out.println("error loginServlet getUserGs catch lineNumRs.close() exception:"+e.getMessage());
//					}
//					conn = null;
//				}
//			}		
		//}
						
		String gsinfo = getUserGs(conn,userid);
		if(gsinfo != null){
			return gsinfo;
		}else{
			return "1"; 
		}
		
		//out.close();							   	
    }
    
    private String getUserGs(Connection conn,String userid){
		String sql= "select g.gsid as id,g.gsname as name from gsid_userid u,gs g  where u.userid="+userid+" and g.gsid=u.gsid;";
		ResultSet lineNumRs=retrySelect(conn,sql);
		try{
			if(lineNumRs.next()){
				String outstr = lineNumRs.getString("id")+"@$"+lineNumRs.getString("name");		
				System.out.println("gsinfo:"+outstr);
				return outstr;
			}else{
				return null;
			}
		}catch(Exception e){
			System.out.println("error loginServlet getUserGs exception:"+e.getMessage());
			return null;
		}finally{
			if(lineNumRs != null){
				try {
					lineNumRs.close();
				} catch (Exception e) {					
					System.out.println("error loginServlet getUserGs catch lineNumRs.close() exception:"+e.getMessage());
				}
				lineNumRs = null;
			}									
		}
    }
    
    //return userid
	private String checkuser(HttpServletRequest request,Map<String,String> cookiesMap,Connection conn){
		String username=request.getParameter("userid");
		String pass=request.getParameter("password");
		
		if(username == null) username=cookiesMap.get("u");
		
		if(pass == null) pass=cookiesMap.get("k");
		
		if(username == null || pass == null) return null;
		
		int countLineNum = 0;
		String userid = null;
		
		//String sql= "select count(1) as cun,password('"+password+"') as passwdEncode from userdb.passwd where ip='"+ip+"' and userid='"+userid+"' and passwd=password('"+password+"');";
		String sql= "select password('"+pass+"') as passwdEncode,userid from userdb.user  where username='"+username+"' and passwd=password('"+pass+"');";
		ResultSet lineNumRs=retrySelect(conn,sql);
		try{
			if(lineNumRs.next()){				
				userid=lineNumRs.getString("userid");
				System.out.println("checkuser ok,countLineNum:"+countLineNum+",username:"+username+",pass:"+pass);
				return userid;
			}else{
				System.out.println("error! checkuser false,countLineNum:"+countLineNum+",username:"+username+",pass:"+pass);
				return null;
			}						
		}catch(Exception e){
			System.out.println("error loginServlet checkuser exception:"+e.getMessage());
			return null;
		}finally{
			if(lineNumRs != null){
				try {
					lineNumRs.close();
				} catch (Exception e) {					
					System.out.println("error loginServlet checkuser catch lineNumRs.close() exception:"+e.getMessage());
				}
				lineNumRs = null;
			}			
		}
	}
	
	private Connection retryGetConn(){
		Connection conn=null;
		int reconi=1;
        while(reconi<=reconnNum){  
        	conn = getConn();
        	if(conn != null){
        		return conn;
        	}
        	
        	try{
                Thread.sleep(sleeptime);
                }catch(InterruptedException e1){
                	e1.printStackTrace();                    	 
                }
        	reconi++;
        }
        
        return null;
	}
	
	private Connection getConn(){
		Connection conn=null;
		try{
			conn=DriverManager.getConnection(MYSQLURLHead+mysqlip+":"+mysqlport+"/"+dbname+MYSQLURLEnding+ConnLanCode+URLTimeout+conntimeout,user_privi,user_privi_passwd); 
			return conn;
		}catch(Exception e){
			if(conn != null){    	
            	try{
            		conn.close();
            		conn = null;
            	}catch(Exception e3){
            		System.out.println("select catch exception 3:"+e3.getMessage()); 
            	}
            } 			
			return null;
		}				
	}	
	
	private ResultSet retrySelect(Connection conn,String sql){
		ResultSet result=null;		
		int reconi=1;
        while(reconi<=reconnNum){  
        	result= select(conn,sql);
    		if(result != null) {
    			return result;
    		}
        	
        	try{
                Thread.sleep(sleeptime);
                }catch(InterruptedException e1){
                	System.out.println("error retrySelect thread sleep exception:"+e1.getMessage());                    	 
                }
        	
        	try {
        		if(conn != null){
        			conn.close();
        		}								
			} catch (Exception e) {				
				System.out.println("error retrySelect conn close exception:"+e.getMessage());  
			}
        	conn=null;
        	conn=retryGetConn();
        	reconi++;         	
        }       
        return null;
	}
	
	private ResultSet select(Connection conn,String sql){		
		ResultSet res=null;		      	
		try {        			
    		Statement st = conn.createStatement();
        	//st.execute("set names "+SetNameLanCode+";");
        	res=st.executeQuery(sql);           	
            return res;
    	}catch(Exception e){            	
            System.out.println("error select exception:"+e.getMessage());                
            if(res != null){
            	try{
            		res.close();
            	}catch(Exception e2){
            		System.out.println("error select catch exception 2:"+e2.getMessage()); 
            	}
            	res = null;
            }
            return null;
        }		
	}
	
//	private String printMenuHead(String headtext){
//		return String.format("{text:\"%s\",expanded:true,children:[",headtext);
//	}
//	
//	private String printMenuLeaf(String leafid,String leaftext){
//		return String.format("{id:\"%s\",text:\"%s\",leaf:true},",leafid,leaftext);
//	}
	
	private String printMenuHead(String headtext){
		return String.format("{\"text\":\"%s\",\"expanded\":\"true\",\"children\":[",headtext);
	}
	
	private String printMenuLeaf(String leafid,String leaftext){
		return String.format("{\"id\":\"%s\",\"text\":\"%s\",\"leaf\":\"true\"},",leafid,leaftext);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	dojob(request,response);
	}	
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		dojob(request, response);
	}	
	
	private Map<String,String> analyseCookies(HttpServletRequest request){
    	Map<String,String> cookiesMap=new HashMap<>();
    	Cookie[] cookies = request.getCookies();
    	int len = cookies.length;
    	for(int i=0;i<len;i++){
    		cookiesMap.put(cookies[i].getName(), cookies[i].getValue());
		}
    	return cookiesMap;
    }	   
	
	static{
    	try{
    		Class.forName(MYSQLDRIVERNAME);
    	}catch(ClassNotFoundException e){
    		e.printStackTrace();
    	}
    }
	
	private String getRemortIP(HttpServletRequest request){
		if (request.getHeader("x-forwarded-for") == null) {
			return request.getRemoteAddr();
			}
			return request.getHeader("x-forwarded-for");
	}
	
	private String decode(String str){
		if(str == null) return null;
		if("cfgnull".equals(str)) return "cfgnull";
		
		str = str.replaceAll(" ","+").replaceAll("%2F","/").replaceAll("%3D","=");
		String res = null;
		try {
			res = new String(decoder.decodeBuffer(str));
		} catch (IOException e1) {
			System.out.println(e1.getMessage());
		}
		return res;
	}

}
