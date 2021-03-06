package com.template;

import com.annotation.Template;
import com.template.templates.AbstractTemplate;
import com.util.ReflectionUtil;
import com.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TemplateManager {
    @Autowired
    private TemplateLoader loader;

    /**
     * 加载模板数据的过程. 热更新重新加载也是调用此方法
     */
    @PostConstruct
    public void load() {
        List<Class<?>> scan=ReflectionUtil.scan(null,Template.class,"com.template.templates");
        for(Class<?> clazz : scan){
            Object o=null;
            try{
                o=clazz.newInstance();
            } catch(InstantiationException | IllegalAccessException e){
                log.error("",e);
            }

            if (!(o instanceof AbstractTemplate)) {
                log.error("非模板类 = {}", clazz);
                continue;
            }
            Template templateAnno = o.getClass().getAnnotation(Template.class);

            String path = System.getProperty("user.dir")
                    + File.separator
                    + "bin"
                    + File.separator
                    + "templates"
                    + File.separator
                    + templateAnno.path();

            Class<? extends AbstractTemplate> subclass = o.getClass().asSubclass(AbstractTemplate.class);
            loader.loadTemplate(new File(path), subclass);
        }
    }
}
