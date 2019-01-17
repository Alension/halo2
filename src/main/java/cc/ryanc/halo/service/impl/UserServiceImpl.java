package cc.ryanc.halo.service.impl;

import cc.ryanc.halo.model.domain.User;
import cc.ryanc.halo.model.dto.Code2SessionResp;
import cc.ryanc.halo.model.dto.JsonResult;
import cc.ryanc.halo.model.dto.Site;
import cc.ryanc.halo.model.enums.BlogPropertiesEnum;
import cc.ryanc.halo.model.enums.MiniProgramPropertiesEnum;
import cc.ryanc.halo.model.enums.PostStatusEnum;
import cc.ryanc.halo.model.enums.PostTypeEnum;
import cc.ryanc.halo.model.enums.ResponseStatusEnum;
import cc.ryanc.halo.model.enums.TrueFalseEnum;
import cc.ryanc.halo.model.request.UserR;
import cc.ryanc.halo.repository.PostRepository;
import cc.ryanc.halo.repository.UserRepository;
import cc.ryanc.halo.service.OptionsService;
import cc.ryanc.halo.service.UserService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * <pre>
 *     用户业务逻辑实现类
 * </pre>
 *
 * @author : RYAN0UP
 * @date : 2017/11/14
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OptionsService optionsService;

    @Autowired
    private PostRepository postRepository;
    /**
     * 保存个人资料
     *
     * @param user user
     */
    @Override
    public void save(User user) {
        userRepository.save(user);
    }

    /**
     * 根据用户名和密码查询
     *
     * @param userName userName
     * @param userPass userPass
     * @return User
     */
    @Override
    public User userLoginByName(String userName, String userPass) {
        return userRepository.findByUserNameAndUserPass(userName, userPass);
    }

    /**
     * 根据邮箱和密码查询，用户登录
     *
     * @param userEmail userEmail
     * @param userPass  userPass
     * @return User
     */
    @Override
    public User userLoginByEmail(String userEmail, String userPass) {
        return userRepository.findByUserEmailAndUserPass(userEmail, userPass);
    }

    /**
     * 查询所有用户
     *
     * @return User
     */
    @Override
    public User findUser() {
        final List<User> users = userRepository.findAll();
        if (users != null && users.size() > 0) {
            return users.get(0);
        } else {
            return new User();
        }
    }

    /**
     * 验证修改密码时，密码是否正确
     *
     * @param userId   userId
     * @param userPass userPass
     * @return User
     */
    @Override
    public User findByUserIdAndUserPass(Long userId, String userPass) {
        return userRepository.findByUserIdAndUserPass(userId, userPass);
    }

    /**
     * 修改禁用状态
     *
     * @param enable enable
     */
    @Override
    public void updateUserLoginEnable(String enable) {
        final User user = this.findUser();
        user.setLoginEnable(enable);
        userRepository.save(user);
    }

    /**
     * 修改最后登录时间
     *
     * @param lastDate 最后登录时间
     * @return User
     */
    @Override
    public User updateUserLoginLast(Date lastDate) {
        final User user = this.findUser();
        user.setLoginLast(lastDate);
        userRepository.save(user);
        return user;
    }

    /**
     * 增加登录错误次数
     *
     * @return 登录错误次数
     */
    @Override
    public Integer updateUserLoginError() {
        final User user = this.findUser();
        user.setLoginError((user.getLoginError() == null ? 0 : user.getLoginError()) + 1);
        userRepository.save(user);
        return user.getLoginError();
    }

    /**
     * 修改用户的状态为正常
     *
     * @return User
     */
    @Override
    public User updateUserNormal() {
        final User user = this.findUser();
        user.setLoginEnable(TrueFalseEnum.TRUE.getDesc());
        user.setLoginError(0);
        user.setLoginLast(new Date());
        userRepository.save(user);
        return user;
    }

    @Override
    public JsonResult login(UserR userR) {
        final RestTemplate restTemplate = new RestTemplate();

        final String urlFormat = MiniProgramPropertiesEnum.LOGIN_URL_FORMAT.getValue();
        final String appId = optionsService.findOneOption(MiniProgramPropertiesEnum.APP_ID.getValue());
        final String appSecret = optionsService.findOneOption(MiniProgramPropertiesEnum.APP_SECRET.getValue());

        final String url = String.format(urlFormat, appId, appSecret,userR.getJsCode());

        final ResponseEntity<String> entity = restTemplate.getForEntity(url, String.class);
        if (HttpStatus.OK.equals(entity.getStatusCode())){
            final String body = entity.getBody();
            Gson gson = new Gson();
            final Code2SessionResp resp = gson.fromJson(body, Code2SessionResp.class);

            final String openid = resp.getOpenid();

            User user = userRepository.findByOpenid(openid);
            //没有用户记录则新建一个用户
            if (user == null){
                user = new User();
                user.setOpenid(openid);
                user.setUserName(userR.getUserName());
                user.setUserAvatar(userR.getAvatarUrl());
                user.setUserRole(0);
                user = userRepository.save(user);
            }

            return new JsonResult(ResponseStatusEnum.SUCCESS.getCode(), ResponseStatusEnum.SUCCESS.getMsg(),
                    user);
        }
        return new JsonResult(ResponseStatusEnum.ERROR.getCode(), ResponseStatusEnum.ERROR.getMsg());
    }

    @Override
    public JsonResult getSiteInfo() {
        final Integer publishPostNum = postRepository
                .countAllByPostStatusAndPostType(PostStatusEnum.PUBLISHED.getCode(),
                        PostTypeEnum.POST_TYPE_POST.getDesc());

        final Long viewsSum = postRepository.getPostViewsSum();

        final Long likesSum = postRepository.getPostLikesSum();

        Site site =  new Site();
        site.setPublishPostNum(publishPostNum);
        site.setViewNum(viewsSum);
        site.setLikeNum(likesSum);
        return new JsonResult(ResponseStatusEnum.SUCCESS.getCode(),site);
    }
}
