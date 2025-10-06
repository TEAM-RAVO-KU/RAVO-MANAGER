package org.ravo.client.controller;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * DB 장애 테스트용 컨트롤러
 */
@Controller
@RequestMapping("/test")
public class TestController {

    /**
     * DB 연결 오류 시뮬레이션
     * 실제 DB 장애를 테스트하기 위한 엔드포인트
     */
    @GetMapping("/db-error")
    public String testDbError() {
        // DataAccessException을 던져서 GlobalExceptionHandler에서 처리하도록 함
        throw new DataAccessResourceFailureException(
            "Unable to acquire JDBC Connection from DataSource. " +
            "Connection pool exhausted or database server is down."
        );
    }

    /**
     * DB 타임아웃 오류 시뮬레이션
     */
    @GetMapping("/db-timeout")
    public String testDbTimeout() {
        throw new QueryTimeoutException(
            "Query timeout: The database query exceeded the maximum allowed time (30 seconds)."
        );
    }

    /**
     * 일반 오류 시뮬레이션
     */
    @GetMapping("/error")
    public String testError() {
        throw new RuntimeException("General system error occurred during processing (Test)");
    }
}
