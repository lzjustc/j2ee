package sc.ustc.controller;

import net.sf.cglib.proxy.Enhancer;
import sc.ustc.di.BeanParser;
import sc.ustc.di.DIHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

public class SimpleController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected synchronized void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html;charset=utf-8");
        //设置Path
        Path.getInstance().setXmlPath(req.getServletContext().getRealPath("/WEB-INF/classes/controller.xml"));
        Path.getInstance().setLogfilePath(req.getServletContext().getRealPath("/WEB-INF/log/log.xml"));
        Path.getInstance().setContextPath(req.getContextPath());
        Path.getInstance().setOrMappingPath(req.getServletContext().getRealPath("/WEB-INF/classes/or_mapping.xml"));
        //获取action名称
        String servletPath = req.getServletPath();
        String actionName = servletPath.replaceAll("\\.sc","").substring(1);
        //解析action
        Action action = null;
       for(Action thisAction:Config.getInstance().getActions()){
            if(actionName.equals(thisAction.getName())){
                //找到action
                action=thisAction;
            }
        }
        if(action == null){
            resp.sendRedirect(req.getServletContext().getContextPath()+ "/pages/error_action.jsp");
            return;
        }
        //调用代理方法
        String value = null;
        try {
            Class cls = Class.forName(action.getClassName());
            Method method = cls.getMethod(action.getMethodName(), HttpServletRequest.class);
            // 无代理
//            Object obj = cls.newInstance();
            //代理
            Object obj = CGLibProxyFactory.getProxy(cls,action);

            //依赖注入
            boolean diResult = DIHandler.dependencyInject(obj,action.getClassName());
            System.out.println("di result: "+diResult);

            value = (String)method.invoke(obj,req);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //执行结果
        for(Result result : action.getResults()){
            if(result.getName().equals(value)){
                String contextPath = Path.getInstance().getContextPath();
                System.out.println("contextPath= "+contextPath);
                if(result.getType().equals("redirect")){
                    resp.sendRedirect(contextPath+"/"+result.getValue());
                    return;
                }
                else if(result.getType().equals("forward")){
                    req.getRequestDispatcher("/"+result.getValue()).forward(req,resp);
                    break;
                }
            }
        }
    }
}
