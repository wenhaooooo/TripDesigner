package com.tripdesigner.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 文档配置。
 *
 * 访问地址：
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI tripDesignerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TripDesigner API")
                        .description("基于多 Agent 协作的 AI 旅行规划后端系统\n\n"
                                + "## 核心能力\n"
                                + "- 8 个专业 Agent 协同生成行程\n"
                                + "- JWT 认证与权限控制\n"
                                + "- 反馈循环：自动从用户交互中学习偏好\n"
                                + "- 工作流取消与进度查询\n\n"
                                + "## 认证方式\n"
                                + "除 `/auth/**` 和公开接口外，所有接口都需要 Bearer Token。\n"
                                + "1. 调用 `POST /auth/register` 或 `POST /auth/login` 获取 Token\n"
                                + "2. 点击右上角 **Authorize** 按钮，输入 `Bearer <access_token>`")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("TripDesigner")
                                .url("https://github.com/tripdesigner"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer Token 认证。在 Authorization 头中传入 `Bearer <token>`")));
    }
}
