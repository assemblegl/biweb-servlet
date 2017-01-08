package gl.servlet;


import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


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
    
    private void dojob(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	System.out.println("in dojob");
    	String key=request.getParameter("skey");
    	System.out.println("key:"+key);
    	//check user
    	if( key != null && "5tgb8ik,".equals(key)){
    		userlogin(request,response);
			return;
		}	
		
		//index.html create menu
		if( key != null && "5tgb7ujm".equals(key)){
			createMenu(request,response);
			return;
		}
		
		if(key != null && "4rfv6yhn".equals(key)){
			getVersion(request,response);
			return;
		}
		
		if(key != null && "4rfv5tgb".equals(key)){
			getGameList(request,response);
			return;
		}
		
		if(key != null && "4rfv3edc".equals(key)){
			getAdvtype(request,response);
			return;
		}
		
		if(key != null && "3edc5tgb".equals(key)){
			getCh(request,response);
			return;
		}
		
		if(key != null && "3edc6yhn".equals(key)){
			getSCh(request,response);
			return;
		}
		
    }
    
    private void getSCh(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
    	response.setContentType("text/json;charset=gbk");
		PrintWriter out = response.getWriter();
		
    	String username=request.getParameter("userid");
		String password=request.getParameter("password");  //passwdEncode
		String gamename = request.getParameter("game");
		System.out.println("sch game:"+gamename);
		String userid = checkuser(username, password);
		if( userid ==  null){
			//login false
			System.out.println("error! getSCh userid:"+username+",pass:"+password+",check false.");
			//out.print("["+accountMenuError+"]"); 
			out.flush();
			out.close();
			return;
		}
		
    	Connection conn = retryGetConn();
		if(conn == null){
			System.out.print("error! getGameList retryGetConn conn is null");
			//out.print("["+accountMenuError+"]"); 
			out.flush();
			out.close();
			return;
		}
		
		//get default game,if not set game
		if(gamename == null || "cfgnull".equals(gamename)){
			ResultSet gameRs = retrySelect(conn,"select g.gid,g.gamename from userid_gid u,game g where u.gid=g.gid and u.userid="+userid+" limit 1;");
			try {
				if(gameRs.next()){
					gamename = gameRs.getString("gamename");
					System.out.println("gamename do not set,get defalut gamename:"+gamename);
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
				out.print(outstr);
				out.flush();
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
			
			if(conn !=null){
				try {
					conn.close();
				} catch (Exception e) {
					System.out.println("error loginServlet getSCh finally conn.close() exception:"+e.getMessage());
				}
				conn = null;
			}
			
			if(out != null){
				out.close();
			}
		}
    }
    
    private void getCh(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
    	response.setContentType("text/json;charset=gbk");
		PrintWriter out = response.getWriter();		
    	String username = null;
		String password = null;  
		String gamename = null;
		Cookie[] cookies = request.getCookies();
		int len = cookies.length;
		for(int i=0;i<len;i++){
			switch (cookies[i].getName()){
				case "u":username=cookies[i].getValue();break;
				case "k":password=cookies[i].getValue();break;
				case "gn":gamename=cookies[i].getValue();break;
			}
		}
		
		System.out.println("ch game:"+gamename);
		String userid = checkuser(username, password);
		if( userid ==  null){
			//login false
			System.out.println("error! getCh userid:"+username+",pass:"+password+",check false.");
			//out.print("["+accountMenuError+"]"); 
			out.flush();
			out.close();
			return;
		}
		
    	Connection conn = retryGetConn();
		if(conn == null){
			System.out.print("error! getGameList retryGetConn conn is null");
			//out.print("["+accountMenuError+"]"); 
			out.flush();
			out.close();
			return;
		}
		
		//get default game,if not set game
		if(gamename == null || "cfgnull".equals(gamename)){
			ResultSet gameRs = retrySelect(conn,"select g.gid,g.gamename from userid_gid u,game g where u.gid=g.gid and u.userid="+userid+" limit 1;");
			try {
				if(gameRs.next()){
					gamename = gameRs.getString("gamename");
					System.out.println("gamename do not set,get defalut gamename:"+gamename);
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
		}
		
		ResultSet menuRs = null;
		String outstr = "{\"ch\":[";
		int init = 0;
		try{
			if(!"cfgnull".equals(gamename)){
				menuRs = retrySelect(conn,"select c.chid,c.chname from game g,gid_chid gc,ch c  where g.gid=gc.gid and c.chid=gc.chid and g.gamename='"+gamename+"';");
				while(menuRs.next()){
					if(init == 0){
						outstr+="{\"id\":-1,\"name\":\"所有渠道\",\"boolean\":true},";
						init = 1;
					}else{
						outstr +=",";
						init++;
					}
					outstr+="{\"id\":"+menuRs.getString("chid")+",\"name\":\""+menuRs.getString("chname")+"\",\"boolean\":false}";					
				}
				outstr+="]}";
				System.out.println("chlist:"+outstr);
				out.print(outstr);
				out.flush();
			}			
		}catch(SQLException e){
			System.out.println("error! loginServlet getCh exception:"+e.getMessage());
		}finally{
			if(menuRs != null){
				try {
					menuRs.close();
				} catch (Exception e) {					
					System.out.println("error loginServlet getCh finally priviRs.close() exception:"+e.getMessage());
				}
				menuRs = null;
			}
			
			if(conn !=null){
				try {
					conn.close();
				} catch (Exception e) {
					System.out.println("error loginServlet getCh finally conn.close() exception:"+e.getMessage());
				}
				conn = null;
			}
			
			if(out != null){
				out.close();
			}
		}
    }
    
    private void getAdvtype(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
    	response.setContentType("text/json;charset=gbk");
		PrintWriter out = response.getWriter();
		
    	String username=request.getParameter("userid");
		String password=request.getParameter("password");  //passwdEncode
		//String ip=getRemortIP(request);
		String userid = checkuser(username, password);
		if( userid ==  null){
			//login false
			System.out.println("error! createMenu userid:"+username+",pass:"+password+",check false.");
			//out.print("["+accountMenuError+"]"); 
			out.flush();
			out.close();
			return;
		}
		
    	Connection conn = retryGetConn();
		if(conn == null){
			System.out.print("error! getGameList retryGetConn conn is null");
			//out.print("["+accountMenuError+"]"); 
			out.flush();
			out.close();
			return;
		}
		
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
			out.print(outstr);
			out.flush();
		}catch(SQLException e){
			System.out.println("error! loginServlet getAdvtype exception:"+e.getMessage());
		}finally{
			if(menuRs != null){
				try {
					menuRs.close();
				} catch (Exception e) {					
					System.out.println("error loginServlet getAdvtype finally priviRs.close() exception:"+e.getMessage());
				}
				menuRs = null;
			}
			
			if(conn !=null){
				try {
					conn.close();
				} catch (Exception e) {
					System.out.println("error loginServlet getAdvtype finally conn.close() exception:"+e.getMessage());
				}
				conn = null;
			}
			
			if(out != null){
				out.close();
			}
		}
    }
    
    private void getGameList(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
    	response.setContentType("text/json;charset=gbk");
		PrintWriter out = response.getWriter();
		
    	String username=request.getParameter("userid");
		String password=request.getParameter("password");  //passwdEncode
		//String ip=getRemortIP(request);
		String userid = checkuser(username, password);
		if( userid ==  null){
			//login false
			System.out.println("error! createMenu userid:"+username+",pass:"+password+",check false.");
			//out.print("["+accountMenuError+"]"); 
			out.flush();
			out.close();
			return;
		}
		
    	Connection conn = retryGetConn();
		if(conn == null){
			System.out.print("error! getGameList retryGetConn conn is null");
			//out.print("["+accountMenuError+"]"); 
			out.flush();
			out.close();
			return;
		}
		
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
			out.print(outstr);
			out.flush();
		}catch(SQLException e){
			System.out.println("error! loginServlet getGameList exception:"+e.getMessage());
		}finally{
			if(menuRs != null){
				try {
					menuRs.close();
				} catch (Exception e) {					
					System.out.println("error loginServlet getVersion finally priviRs.close() exception:"+e.getMessage());
				}
				menuRs = null;
			}
			
			if(conn !=null){
				try {
					conn.close();
				} catch (Exception e) {
					System.out.println("error loginServlet getVersion finally conn.close() exception:"+e.getMessage());
				}
				conn = null;
			}
			
			if(out != null){
				out.close();
			}
		}
    }
    
    private void getVersion(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
    	response.setContentType("text/json;charset=gbk");
		PrintWriter out = response.getWriter();
		
    	String username=request.getParameter("userid");
		String password=request.getParameter("password");  //passwdEncode
		//String ip=getRemortIP(request);
		String userid = checkuser(username, password);
		if( userid ==  null){
			//login false
			System.out.println("error! createMenu userid:"+username+",pass:"+password+",check false.");
			//out.print("["+accountMenuError+"]"); 
			out.flush();
			out.close();
			return;
		}
		
		Connection conn = retryGetConn();
		if(conn == null){
			System.out.print("error! getVersion retryGetConn conn is null");
			out.flush();
			out.close();
			return;
		}
		
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
			out.print(outstr);
			out.flush();
		}catch(SQLException e){
			System.out.println("error! loginServlet getVersion exception:"+e.getMessage());
		}finally{
			if(menuRs != null){
				try {
					menuRs.close();
				} catch (Exception e) {					
					System.out.println("error loginServlet getVersion finally priviRs.close() exception:"+e.getMessage());
				}
				menuRs = null;
			}
			
			if(conn !=null){
				try {
					conn.close();
				} catch (Exception e) {
					System.out.println("error loginServlet getVersion finally conn.close() exception:"+e.getMessage());
				}
				conn = null;
			}
			
			if(out != null){
				out.close();
			}
		}					
    }
    
    private void createMenu(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
    	response.setContentType("text/json;charset=gbk");
		PrintWriter out = response.getWriter();
		
    	String username=request.getParameter("userid");
		String password=request.getParameter("password");  //passwdEncode
		//String ip=getRemortIP(request);
		String userid = checkuser(username, password);
		if( userid == null){
			//login false
			System.out.println("error! createMenu userid:"+username+",pass:"+password+",check false.");
			out.print("["+accountMenuError+"]"); 
			out.flush();
			out.close();
			return;
		}
		
		Connection conn = retryGetConn();
		if(conn == null){
			System.out.print("error! retryGetConn conn is null");
			out.print("["+accountMenuError+"]"); 
			out.flush();
			out.close();
			return;
		}		
		
		//String sql = "select menuleafkey,menuleaftext,menuheadtext,menuheadkey,isleaf from privi p,menuleaf b,menuhead c where p.leafid=b.id and b.menuheadid=c.id and userid='"+username+"' order by menuheadid,b.id;";	
		String sql="select menuleafkey,menuleaftext,menuheadtext,menuheadkey,isleaf from privi p,menuleaf l,menuhead h where p.leafid=l.id and l.menuheadid=h.id and p.userid='"+userid+"' order by menuheadid,l.id;";
		String preMenuHead = null;
		//int preHeadLeaf = 0;
		String nowMenuHead="";
		//String menuLeaf="";
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
//			String preMenuHead="";
//			int preHeadLeaf = 0;
//			String nowMenuHead="";
//			String menuLeaf="";
//			String menuAllstr="";
			
//		try{														
//			while(menuRs.next()){
//				nowMenuHead=menuRs.getString("menuheadtext");
//				preHeadLeaf = 
//				//menuhead not change
//				if(preMenuHead.equals(nowMenuHead) || preMenuHead.equals("")){
//					preMenuHead=nowMenuHead;
//					
//					//add leaf
//					menuLeaf+=printMenuLeaf(menuRs.getString("menuleafid"),menuRs.getString("menuleaftext"));										
//				//menuhead change	
//				}else{
//					//add head and leaf
//					
//					//head leaf
//					if(menuRs.getInt("isleaf") == 1){
//						menuAllstr+=printMenuLeaf(menuRs.getString("menuheadkey"),menuRs.getString("menuheadtext"));
//					}else{
//						menuAllstr+=printMenuHead(preMenuHead)+menuLeaf.replaceAll(",$","")+"]},";
//					}	
//					menuLeaf=printMenuLeaf(menuRs.getString("menuleafid"),menuRs.getString("menuleaftext"));
//					preMenuHead=nowMenuHead;
//				}					
//			}
//			
//			if(menuRs.getInt("isleaf") == 1){
//				menuAllstr+=printMenuLeaf(menuRs.getString("menuheadkey"),menuRs.getString("menuheadtext"));
//			}else{
//				menuAllstr+=printMenuHead(preMenuHead)+menuLeaf.replaceAll(",$","")+"]},";
//			}		
//			menuAllstr="["+menuAllstr.replaceAll(",$","")+"]";
			
			System.out.println("create menu:"+menuAllstr);
			//out.print("{children:[{id:\"SpreadOverView\",text:\"推广概览\",leaf:true},{id:\"SpreadEvents\",text:\"推广活动\",leaf:true}]}");
			//out.print("[{id:\"SpreadOverView\",text:\"推广概览\",leaf:true},{id:\"SpreadEvents\",text:\"推广活动\",leaf:true}]");
			out.print(menuAllstr); 
			out.flush();			
		}catch(SQLException e){
			System.out.println("error! loginServlet createMenu exception:"+e.getMessage());
		}finally{
			if(menuRs != null){
				try {
					menuRs.close();
				} catch (Exception e) {					
					System.out.println("error loginServlet createMenu finally priviRs.close() exception:"+e.getMessage());
				}
				menuRs = null;
			}
			
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
	
    private void userlogin(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
    	response.setContentType("text/json;charset=gbk");
		PrintWriter out = response.getWriter();
		
		String username=request.getParameter("userid");
		String password=request.getParameter("password");
		String userid = checkuser(username, password);
		if( userid != null){
			String gsinfo = getUserGs(userid);
			if(gsinfo != null){
				out.print(username+"@$"+password+"@$"+gsinfo);
				out.flush();
				out.close();
				return;
			}
			
		}
		out.print("1"); 
		out.flush();
		out.close();					   	
    }
    
    private String getUserGs(String userid){
    	Connection conn = retryGetConn();
    	if(conn == null){
			System.out.print("error reyGetConn conn is null");
			return null;
		}
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
			
			if(conn !=null){
				try {
					conn.close();
				} catch (Exception e) {
					System.out.println("error loginServlet getUserGs catch conn.close() exception:"+e.getMessage());
				}
				conn = null;
			}			
		}
    }
    
    //return userid
	private String checkuser(String username, String pass){
		if(username == null || pass == null) return null;
		
		int countLineNum = 0;
		String passwdEncode=null;
		String userid = null;
		//String ip=getRemortIP(request);
		Connection conn = retryGetConn();
		if(conn == null){
			System.out.print("error reyGetConn conn is null");
			return null;
		}
		//String sql= "select count(1) as cun,password('"+password+"') as passwdEncode from userdb.passwd where ip='"+ip+"' and userid='"+userid+"' and passwd=password('"+password+"');";
		String sql= "select password('"+pass+"') as passwdEncode,userid from userdb.user  where username='"+username+"' and passwd=password('"+pass+"');";
		ResultSet lineNumRs=retrySelect(conn,sql);
		try{
			if(lineNumRs.next()){
				//countLineNum=lineNumRs.getInt("cun");
				passwdEncode=lineNumRs.getString("passwdEncode");
				userid=lineNumRs.getString("userid");
				System.out.println("countLineNum:"+countLineNum);
				return userid;
			}else{
				return null;
			}
			
//			if(countLineNum >=1 && passwdEncode != null){
//				System.out.println("username:"+username+",pass:"+pass+",check ok!");
//				return userid;
//				
//			}else{
//				//login false
//				System.out.println("error username:"+username+",pass:"+pass+",check false.");
//				return null;			
//			}		
			
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
			
			if(conn !=null){
				try {
					conn.close();
				} catch (Exception e) {
					System.out.println("error loginServlet checkuser catch conn.close() exception:"+e.getMessage());
				}
				conn = null;
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

}
