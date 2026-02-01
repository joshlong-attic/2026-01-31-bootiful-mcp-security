package com.example.mcp_client;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import org.springaicommunity.mcp.security.client.sync.AuthenticationMcpTransportContextProvider;
import org.springaicommunity.mcp.security.client.sync.oauth2.http.client.OAuth2AuthorizationCodeSyncHttpRequestCustomizer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;
import java.util.List;

@SpringBootApplication
public class McpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpClientApplication.class, args);
    }


    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)  {
        return http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Client(Customizer.withDefaults())
                .build();
    }

    @Bean
    McpSyncHttpClientRequestCustomizer mcpSyncHttpClientRequestCustomizer(OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
        return new OAuth2AuthorizationCodeSyncHttpRequestCustomizer(oAuth2AuthorizedClientManager, "authserver");
    }

    @Bean
    McpSyncClientCustomizer mcpSyncClientCustomizer() {
        return (name, syncSpec) -> syncSpec
                .transportContextProvider(new AuthenticationMcpTransportContextProvider());
    }
}

@Controller
@ResponseBody
class McpClientController {

    private final ChatClient ai;

    private final SyncMcpToolCallbackProvider toolCallbackProvider;

    McpClientController(List<McpSyncClient> syncClientList, ChatClient.Builder ai) {
        this.toolCallbackProvider = SyncMcpToolCallbackProvider
                .builder() //
                .mcpClients(syncClientList) //
                .build();
        this.ai = ai.defaultSystem(
            """
                    You are an AI powered assistant to help people adopt a dog from the adoptions agency 
                    named Pooch Palace with locations in Antwerp, Seoul, Tokyo, Singapore, Paris, Mumbai, New Delhi, 
                    Barcelona, San Francisco, and London. Information about the dogs availables will be presented below. 
                    If there is no information, then return a polite response suggesting wes don't have any dogs available.
                    
                    If somebody asks for a time to pick up the dog, don't ask other questions: simply provide a time by 
                    consulting the tools you have available.
                """
            )
            .build();
    }


    @GetMapping("/")
    DogAdoptionAppointment ask() {
        return this.ai
                .prompt()
                .toolCallbacks(this.toolCallbackProvider)
                .user("""
                        when might I pick up Prancer the poodle for adoption 
                        from the San Francisco Pooch Palace location? Give me a date.
                        """)
                .call()
                .entity(DogAdoptionAppointment.class);
    }
}

record DogAdoptionAppointment(
        Instant when, String dogName, String clientName
) {
}

