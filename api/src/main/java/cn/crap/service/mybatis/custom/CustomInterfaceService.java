package cn.crap.service.mybatis.custom;

import cn.crap.dao.mybatis.InterfaceMapper;
import cn.crap.dao.mybatis.ProjectMapper;
import cn.crap.dao.mybatis.custom.CustomProjectMapper;
import cn.crap.dto.ErrorDto;
import cn.crap.dto.InterfacePDFDto;
import cn.crap.dto.ParamDto;
import cn.crap.dto.ResponseParamDto;
import cn.crap.enumeration.LogType;
import cn.crap.model.mybatis.*;
import cn.crap.model.mybatis.ProjectCriteria;
import cn.crap.service.ICacheService;
import cn.crap.service.mybatis.imp.MybatisLogService;
import cn.crap.service.mybatis.imp.MybatisModuleService;
import cn.crap.springbeans.Config;
import cn.crap.utils.MyString;
import cn.crap.utils.Page;
import cn.crap.utils.TableField;
import cn.crap.utils.Tools;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CustomInterfaceService {
    @Autowired
    private InterfaceMapper mapper;

    @Autowired
    private ICacheService cacheService;
    @Autowired
    private MybatisModuleService moduleService;
    @Autowired
    private MybatisLogService logService;

    public void getInterDto(Config config, List<InterfacePDFDto> interfaces, InterfaceWithBLOBs interFace, InterfacePDFDto interDto) {
        interDto.setModel(interFace);
        if(interFace.getParam().startsWith("form=")){
            interDto.setFormParams(JSONArray.toArray(JSONArray.fromObject(interFace.getParam().substring(5)),ParamDto.class));
        }else{
            interDto.setCustomParams( interFace.getParam());
        }
        interDto.setTrueMockUrl(config.getDomain()+"/mock/trueExam.do?id="+interFace.getId());
        interDto.setFalseMockUrl(config.getDomain()+"/mock/falseExam.do?id="+interFace.getId());

        interDto.setHeaders( JSONArray.toArray(JSONArray.fromObject(interFace.getHeader()),ParamDto.class));
        interDto.setResponseParam( JSONArray.toArray(JSONArray.fromObject(interFace.getResponseParam()),ResponseParamDto.class) );
        interDto.setParamRemarks( JSONArray.toArray(JSONArray.fromObject(interFace.getParamRemark()), ResponseParamDto.class) );
        interDto.setErrors( JSONArray.toArray(JSONArray.fromObject(interFace.getErrors()),ErrorDto.class) );
        interfaces.add(interDto);
    }


//	@Override
//	@Transactional
//	public JsonResult getInterfaceList(Page page,List<String> moduleIds, Interface interFace, Integer currentPage) {
//		Map<String, Object> params = Tools.getMap("moduleId", interFace.getModuleId(),
//				"interfaceName|like", interFace.getInterfaceName(),"fullUrl|like", interFace.getUrl()==null?"":interFace.getUrl().trim());
//		if(moduleIds != null){
//			moduleIds.add("NULL");// 防止长度为0，导致in查询报错
//			params.put("moduleId|in", moduleIds);
//		}
//
//		List<Interface> interfaces = findByMap(
//				params, " new Interface(id,moduleId,interfaceName,version,createTime,updateBy,updateTime,remark,sequence)", page, null);
//
//		List<Module> modules = new ArrayList<Module>();
//		// 搜索接口时，modules为空
//		if (interFace.getModuleId() != null && MyString.isEmpty(interFace.getInterfaceName()) && MyString.isEmpty(interFace.getUrl()) ) {
//			ModuleCriteria example = new ModuleCriteria();
//			example.createCriteria().andPa
//
//			params = Tools.getMap("parentId", interFace.getModuleId(), "type", "MODULE");
//			if(moduleIds != null){
//				moduleIds.add("NULL");// 防止长度为0，导致in查询报错
//				params.put("id|in", moduleIds);
//			}
//			params.put("id|!=", "top");// 顶级目录不显示
//
//			modules = moduleService.findByMap(params, null, null);
//		}
//		params.clear();
//		params.put("interfaces", interfaces);
//		params.put("modules", modules);
//		return new JsonResult(1, params, page,
//				Tools.getMap("crumbs", Tools.getCrumbs("接口列表:"+cacheService.getModuleName(interFace.getModuleId()),"void"),
//						"module",cacheService.getModule(interFace.getModuleId())));
//	}

    public void getInterFaceRequestExam(InterfaceWithBLOBs interFace) {
        Module module = cacheService.getModule(interFace.getModuleId());
        interFace.setRequestExam("请求地址:"+ module.getName() + interFace.getUrl()+"\r\n");

        // 请求头
        JSONArray headers = JSONArray.fromObject(interFace.getHeader());
        StringBuilder strHeaders = new StringBuilder("请求头:\r\n");
        JSONObject obj = null;
        for(int i=0;i<headers.size();i++){
            obj = (JSONObject) headers.get(i);
            strHeaders.append("\t"+obj.getString("name") + "="+ (obj.containsKey("def")?obj.getString("def"):"")+"\r\n");
        }

        // 请求参数
        StringBuilder strParams = new StringBuilder("请求参数:\r\n");
        if(!MyString.isEmpty(interFace.getParam())){
            JSONArray params = null;
            if(interFace.getParam().startsWith("form=")){
                params = JSONArray.fromObject(interFace.getParam().substring(5));
                for(int i=0;i<params.size();i++){
                    obj = (JSONObject) params.get(i);
                    if(obj.containsKey("inUrl") && obj.getString("inUrl").equals("true")){
                        interFace.setRequestExam(interFace.getRequestExam().replace("{"+obj.getString("name")+"}", (obj.containsKey("def")?obj.getString("def"):"")));
                    }else{
                        strParams.append("\t"+obj.getString("name") + "=" + (obj.containsKey("def")?obj.getString("def"):"")+"\r\n");
                    }
                }
            }else{
                strParams.append(interFace.getParam());
            }
        }
        interFace.setRequestExam(interFace.getRequestExam()+strHeaders.toString()+strParams.toString());
    }


    public String getLuceneType() {
        return "接口";
    }

    public List<InterfaceWithBLOBs> getByProjectId(String projectId) {
        InterfaceCriteria example = new InterfaceCriteria();
        InterfaceCriteria.Criteria criteria = example.createCriteria().andProjectIdEqualTo(projectId);
        return mapper.selectByExampleWithBLOBs(example);
    }

    public int countByFullUrl(String moduleId, String fullUrl, String expectId){
        Assert.notNull(moduleId, "moduleId can't be null");
        Assert.notNull(fullUrl);

        InterfaceCriteria example = new InterfaceCriteria();
        InterfaceCriteria.Criteria criteria = example.createCriteria().andModuleIdEqualTo(moduleId).andFullUrlEqualTo(fullUrl);
        if (expectId != null){
            criteria.andIdNotEqualTo(expectId);
        }
        return mapper.countByExample(example);
    }

    /**
     * update article and add update log
     * @param model
     * @param modelName
     * @param remark
     */
    public void update(InterfaceWithBLOBs model, String modelName, String remark) {
        InterfaceWithBLOBs dbModel = mapper.selectByPrimaryKey(model.getId());
        if(MyString.isEmpty(remark)) {
            remark = model.getInterfaceName();
        }
        // TODO 提取代码
        Log log = new Log();
        log.setModelName(modelName);
        log.setRemark(remark);
        log.setType(LogType.UPDATE.name());
        log.setContent(JSONObject.fromObject(dbModel).toString());
        log.setModelClass(dbModel.getClass().getSimpleName());

        logService.insert(log);
        mapper.updateByPrimaryKeyWithBLOBs(model);
    }

    public void delete(String id, String modelName, String remark){
        Assert.notNull(id);
        InterfaceWithBLOBs dbModel = mapper.selectByPrimaryKey(id);
        if(MyString.isEmpty(remark)) {
            remark = dbModel.getInterfaceName();
        }
        Log log = new Log();
        log.setModelName(modelName);
        log.setRemark(remark);
        log.setType(LogType.DELTET.name());
        log.setContent(JSONObject.fromObject(dbModel).toString());
        log.setModelClass(dbModel.getClass().getSimpleName());

        logService.insert(log);
        mapper.deleteByPrimaryKey(dbModel.getId());
    }
}