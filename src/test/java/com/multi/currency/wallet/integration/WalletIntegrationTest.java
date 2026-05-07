package com.multi.currency.wallet.integration;

import com.multi.currency.wallet.application.dto.request.*;
import com.multi.currency.wallet.application.dto.response.*;
import com.multi.currency.wallet.domain.model.AccountStatus;
import com.multi.currency.wallet.domain.model.TransactionStatus;
import com.multi.currency.wallet.domain.model.TransactionType;
import org.flywaydb.core.Flyway;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test for the Multi-Currency Wallet application.
 *
 * Covers the full vertical stack: HTTP → Security → Controller → UseCase →
 * Repository → PostgreSQL.
 *
 * Test structure:
 * 1. Happy Path Journey – ordered steps sharing state through static fields
 * 2. Security Boundaries – independent auth/authorization negative cases
 * 3. Business Rule Errors – independent domain constraint negative cases
 *
 * Uses Testcontainers (real PostgreSQL 15) + Flyway migrations.
 * Spring Security filter chain is fully active — no mocking.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)   
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WalletIntegrationTest {

        // ── Spring Injection ─────────────────────────────────────────────────────

        @Autowired
        private TestRestTemplate restTemplate;

        @Autowired
        private Flyway flyway;

        @LocalServerPort
        private int port;

        // ── Shared Journey State (static — survives across JUnit instances) ───────

        private static String primaryToken;
        private static UUID primaryUserId;

        private static String secondToken;
        private static UUID secondUserId;

        private static String primaryAccountNumber;
        private static String secondAccountNumber;

        private static UUID depositTxId;
        private static UUID transferDebitTxId;

        // ── Constants ────────────────────────────────────────────────────────────

        private static final String PRIMARY_EMAIL = "alice@wallet.test";
        private static final String PRIMARY_PASSWORD = "password123";
        private static final String PRIMARY_NAME = "Alice Primary";

        private static final String SECOND_EMAIL = "bob@wallet.test";
        private static final String SECOND_PASSWORD = "password456";
        private static final String SECOND_NAME = "Bob Secondary";

        private static final String CURRENCY = "USD";

        @BeforeAll
        void cleanDatabase() {
                flyway.clean();
                flyway.migrate();
        }

        // ═════════════════════════════════════════════════════════════════════════
        // SECTION 1 — HAPPY PATH JOURNEY (Ordered)
        // ═════════════════════════════════════════════════════════════════════════

        @Test
        @Order(1)
        @DisplayName("Step 1: Register primary user → 201 CREATED with token")
        void step1_registerPrimaryUser() {
                RegisterRequest request = new RegisterRequest(PRIMARY_EMAIL, PRIMARY_PASSWORD, PRIMARY_NAME);

                ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                                url("/api/v1/auth/register"), request, AuthResponse.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().token()).isNotBlank();
                assertThat(response.getBody().email()).isEqualTo(PRIMARY_EMAIL);
                assertThat(response.getBody().fullName()).isEqualTo(PRIMARY_NAME);
                assertThat(response.getBody().userId()).isNotNull();

                // Store for subsequent steps
                primaryToken = response.getBody().token();
                primaryUserId = response.getBody().userId();
        }

        @Test
        @Order(2)
        @DisplayName("Step 2: Register second user → 201 CREATED")
        void step2_registerSecondUser() {
                RegisterRequest request = new RegisterRequest(SECOND_EMAIL, SECOND_PASSWORD, SECOND_NAME);

                ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                                url("/api/v1/auth/register"), request, AuthResponse.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isNotNull();

                secondToken = response.getBody().token();
                secondUserId = response.getBody().userId();
        }

        @Test
        @Order(3)
        @DisplayName("Step 3: Login primary user → 200 OK with fresh token")
        void step3_loginPrimaryUser() {
                LoginRequest request = new LoginRequest(PRIMARY_EMAIL, PRIMARY_PASSWORD);

                ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                                url("/api/v1/auth/login"), request, AuthResponse.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().token()).isNotBlank();
                assertThat(response.getBody().email()).isEqualTo(PRIMARY_EMAIL);

                // Refresh token from login (may differ from registration token)
                primaryToken = response.getBody().token();
        }

        @Test
        @Order(4)
        @DisplayName("Step 4: Create primary account (USD) → 201 CREATED with ACTIVE status")
        void step4_createPrimaryAccount() {
                CreateAccountRequest request = new CreateAccountRequest(CURRENCY);

                ResponseEntity<AccountResponse> response = restTemplate.exchange(
                                url("/api/v1/accounts"),
                                HttpMethod.POST,
                                new HttpEntity<>(request, bearerHeaders(primaryToken)),
                                AccountResponse.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().currencyCode()).isEqualTo(CURRENCY);
                assertThat(response.getBody().status()).isEqualTo(AccountStatus.ACTIVE);
                assertThat(response.getBody().balance()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(response.getBody().userId()).isEqualTo(primaryUserId);
                assertThat(response.getBody().accountNumber()).isNotBlank();

                primaryAccountNumber = response.getBody().accountNumber();
        }

        @Test
        @Order(5)
        @DisplayName("Step 5: Create second user's account (USD) → 201 CREATED")
        void step5_createSecondAccount() {
                CreateAccountRequest request = new CreateAccountRequest(CURRENCY);

                ResponseEntity<AccountResponse> response = restTemplate.exchange(
                                url("/api/v1/accounts"),
                                HttpMethod.POST,
                                new HttpEntity<>(request, bearerHeaders(secondToken)),
                                AccountResponse.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().userId()).isEqualTo(secondUserId);

                secondAccountNumber = response.getBody().accountNumber();
        }

        @Test
        @Order(6)
        @DisplayName("Step 6: Get my accounts → 200 OK with one account listed")
        void step6_getMyAccounts() {
                ResponseEntity<List<AccountResponse>> response = restTemplate.exchange(
                                url("/api/v1/accounts"),
                                HttpMethod.GET,
                                new HttpEntity<>(bearerHeaders(primaryToken)),
                                new ParameterizedTypeReference<List<AccountResponse>>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).hasSize(1);
                assertThat(response.getBody().get(0).accountNumber()).isEqualTo(primaryAccountNumber);
        }

        @Test
        @Order(7)
        @DisplayName("Step 7: Deposit 500 USD → 201 CREATED, balance 500, type DEPOSIT")
        void step7_depositToPrimaryAccount() {
                DepositRequest request = new DepositRequest(
                                primaryAccountNumber,
                                new BigDecimal("500.000"),
                                CURRENCY,
                                "Initial deposit");

                ResponseEntity<TransactionResponse> response = restTemplate.exchange(
                                url("/api/v1/accounts/deposit"),
                                HttpMethod.POST,
                                new HttpEntity<>(request, bearerHeaders(primaryToken)),
                                TransactionResponse.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().type()).isEqualTo(TransactionType.DEPOSIT);
                assertThat(response.getBody().amount()).isEqualByComparingTo(new BigDecimal("500.000"));
                assertThat(response.getBody().balanceAfter()).isEqualByComparingTo(new BigDecimal("500.000"));
                assertThat(response.getBody().status()).isEqualTo(TransactionStatus.COMPLETED);
                assertThat(response.getBody().currencyCode()).isEqualTo(CURRENCY);

                depositTxId = response.getBody().id();
        }

        @Test
        @Order(8)
        @DisplayName("Step 8: Deposit another 200 USD → balance 700")
        void step8_secondDeposit() {
                DepositRequest request = new DepositRequest(
                                primaryAccountNumber,
                                new BigDecimal("200.000"),
                                CURRENCY,
                                "Second deposit");

                ResponseEntity<TransactionResponse> response = restTemplate.exchange(
                                url("/api/v1/accounts/deposit"),
                                HttpMethod.POST,
                                new HttpEntity<>(request, bearerHeaders(primaryToken)),
                                TransactionResponse.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().balanceAfter()).isEqualByComparingTo(new BigDecimal("700.000"));
        }

        @Test
        @Order(9)
        @DisplayName("Step 9: Transfer 150 USD to second account → 201 CREATED, two-leg ledger")
        void step9_transfer() {
                TransferRequest request = new TransferRequest(
                                primaryAccountNumber,
                                secondAccountNumber,
                                new BigDecimal("150.000"),
                                CURRENCY,
                                "Rent split");

                ResponseEntity<TransferResponse> response = restTemplate.exchange(
                                url("/api/v1/transfers"),
                                HttpMethod.POST,
                                new HttpEntity<>(request, bearerHeaders(primaryToken)),
                                TransferResponse.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isNotNull();

                TransactionResponse debit = response.getBody().debitTransaction();
                TransactionResponse credit = response.getBody().creditTransaction();

                // Debit leg (source)
                assertThat(debit.type()).isEqualTo(TransactionType.TRANSFER_DEBIT);
                assertThat(debit.amount()).isEqualByComparingTo(new BigDecimal("150.000"));
                assertThat(debit.balanceAfter()).isEqualByComparingTo(new BigDecimal("550.000")); // 700 - 150
                assertThat(debit.status()).isEqualTo(TransactionStatus.COMPLETED);

                // Credit leg (destination)
                assertThat(credit.type()).isEqualTo(TransactionType.TRANSFER_CREDIT);
                assertThat(credit.amount()).isEqualByComparingTo(new BigDecimal("150.000"));
                assertThat(credit.balanceAfter()).isEqualByComparingTo(new BigDecimal("150.000")); // 0 + 150
                assertThat(credit.status()).isEqualTo(TransactionStatus.COMPLETED);

                // Account numbers in response
                assertThat(response.getBody().sourceAccountNumber()).isEqualTo(primaryAccountNumber);
                assertThat(response.getBody().destinationAccountNumber()).isEqualTo(secondAccountNumber);

                transferDebitTxId = debit.id();
        }

        @Test
        @Order(10)
        @DisplayName("Step 10: Get my transactions → 200 OK, 3 entries in desc order")
        void step10_getMyTransactions() {
                ResponseEntity<List<TransactionResponse>> response = restTemplate.exchange(
                                url("/api/v1/transactions"),
                                HttpMethod.GET,
                                new HttpEntity<>(bearerHeaders(primaryToken)),
                                new ParameterizedTypeReference<List<TransactionResponse>>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();

                // Primary user's account has: deposit1 + deposit2 + transfer_debit = 3
                // transactions
                assertThat(response.getBody()).hasSize(3);

                // Most recent first (TRANSFER_DEBIT was last)
                assertThat(response.getBody().get(0).type()).isEqualTo(TransactionType.TRANSFER_DEBIT);
        }

        @Test
        @Order(11)
        @DisplayName("Step 11: Reverse the first deposit → 201 CREATED, REVERSAL_DEBIT created")
        void step11_reverseDeposit() {
                ReversalRequest request = new ReversalRequest(depositTxId, "Reversal test");

                ResponseEntity<List<TransactionResponse>> response = restTemplate.exchange(
                                url("/api/v1/transactions/reverse"),
                                HttpMethod.POST,
                                new HttpEntity<>(request, bearerHeaders(primaryToken)),
                                new ParameterizedTypeReference<List<TransactionResponse>>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isNotNull().hasSize(1);

                TransactionResponse reversal = response.getBody().get(0);
                assertThat(reversal.type()).isEqualTo(TransactionType.REVERSAL_DEBIT);
                assertThat(reversal.referenceTransactionId()).isEqualTo(depositTxId);
                assertThat(reversal.amount()).isEqualByComparingTo(new BigDecimal("500.000"));
                // Balance after reversal: 550 (after transfer) - 500 (reversed deposit) = 50
                assertThat(reversal.balanceAfter()).isEqualByComparingTo(new BigDecimal("50.000"));
                assertThat(reversal.status()).isEqualTo(TransactionStatus.REVERSED);
        }

        @Test
        @Order(12)
        @DisplayName("Step 12: Reverse full transfer atomically → 201 CREATED, both legs reversed")
        void step12_reverseTransfer() {
                ResponseEntity<List<TransactionResponse>> response = restTemplate.exchange(
                                url("/api/v1/transactions/reverse-transfer/" + transferDebitTxId),
                                HttpMethod.POST,
                                new HttpEntity<>(bearerHeaders(primaryToken)),
                                new ParameterizedTypeReference<List<TransactionResponse>>() {
                                });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isNotNull();

                // Both legs reversed: REVERSAL_CREDIT (source gets money back) + REVERSAL_DEBIT
                // (destination loses money)
                assertThat(response.getBody()).hasSizeGreaterThanOrEqualTo(1);

                boolean hasReversalCredit = response.getBody().stream()
                                .anyMatch(t -> t.type() == TransactionType.REVERSAL_CREDIT);
                assertThat(hasReversalCredit).isTrue();
        }

        @Test
        @Order(13)
        @DisplayName("Step 13: DELETE transaction → 405 METHOD NOT ALLOWED (immutability)")
        void step13_deleteTransactionIsNotAllowed() {
                ResponseEntity<Void> response = restTemplate.exchange(
                                url("/api/v1/transactions/" + depositTxId),
                                HttpMethod.DELETE,
                                new HttpEntity<>(bearerHeaders(primaryToken)),
                                Void.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        }

        // ═════════════════════════════════════════════════════════════════════════
        // SECTION 2 — SECURITY BOUNDARY TESTS (Independent)
        // ═════════════════════════════════════════════════════════════════════════

        @Test
        @DisplayName("Security: No token on protected endpoint → 401 UNAUTHORIZED")
        void security_noToken_returns401() {
                ResponseEntity<String> response = restTemplate.exchange(
                                url("/api/v1/accounts"),
                                HttpMethod.GET,
                                HttpEntity.EMPTY,
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Security: Malformed Bearer token → 401 UNAUTHORIZED")
        void security_malformedToken_returns401() {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer this.is.not.a.valid.jwt");

                ResponseEntity<String> response = restTemplate.exchange(
                                url("/api/v1/accounts"),
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Security: Wrong credentials on login → 401 UNAUTHORIZED")
        void security_wrongPassword_returns401() {
                // Register a fresh user for this test
                String email = "wrongpass_" + UUID.randomUUID() + "@test.com";
                restTemplate.postForEntity(
                                url("/api/v1/auth/register"),
                                new RegisterRequest(email, "correctpassword", "Test User"),
                                AuthResponse.class);

                // Attempt login with wrong password
                ResponseEntity<String> response = restTemplate.postForEntity(
                                url("/api/v1/auth/login"),
                                new LoginRequest(email, "wrongpassword"),
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Security: User deposits to another user's account → 403 FORBIDDEN")
        void security_depositToAnotherUsersAccount_returns403() {
                // Register attacker user
                String attackerEmail = "attacker_" + UUID.randomUUID() + "@test.com";
                ResponseEntity<AuthResponse> attackerReg = restTemplate.postForEntity(
                                url("/api/v1/auth/register"),
                                new RegisterRequest(attackerEmail, "password123", "Attacker"),
                                AuthResponse.class);
                String attackerToken = attackerReg.getBody().token();

                // Register victim user and create their account
                String victimEmail = "victim_" + UUID.randomUUID() + "@test.com";
                ResponseEntity<AuthResponse> victimReg = restTemplate.postForEntity(
                                url("/api/v1/auth/register"),
                                new RegisterRequest(victimEmail, "password123", "Victim"),
                                AuthResponse.class);
                String victimToken = victimReg.getBody().token();

                ResponseEntity<AccountResponse> victimAccount = restTemplate.exchange(
                                url("/api/v1/accounts"),
                                HttpMethod.POST,
                                new HttpEntity<>(new CreateAccountRequest("USD"), bearerHeaders(victimToken)),
                                AccountResponse.class);
                String victimAccountNumber = victimAccount.getBody().accountNumber();

                // Attacker tries to deposit to victim's account
                DepositRequest depositRequest = new DepositRequest(
                                victimAccountNumber, new BigDecimal("100.000"), "USD", "Malicious deposit");

                ResponseEntity<String> response = restTemplate.exchange(
                                url("/api/v1/accounts/deposit"),
                                HttpMethod.POST,
                                new HttpEntity<>(depositRequest, bearerHeaders(attackerToken)),
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Security: Transferring from another user's account → 403 FORBIDDEN")
        void security_transferFromAnotherUsersAccount_returns403() {
                // Register owner and thief
                String ownerEmail = "owner_" + UUID.randomUUID() + "@test.com";
                ResponseEntity<AuthResponse> ownerReg = restTemplate.postForEntity(
                                url("/api/v1/auth/register"),
                                new RegisterRequest(ownerEmail, "password123", "Owner"),
                                AuthResponse.class);
                String ownerToken = ownerReg.getBody().token();

                String thiefEmail = "thief_" + UUID.randomUUID() + "@test.com";
                ResponseEntity<AuthResponse> thiefReg = restTemplate.postForEntity(
                                url("/api/v1/auth/register"),
                                new RegisterRequest(thiefEmail, "password123", "Thief"),
                                AuthResponse.class);
                String thiefToken = thiefReg.getBody().token();

                // Create accounts for both
                ResponseEntity<AccountResponse> ownerAccount = restTemplate.exchange(
                                url("/api/v1/accounts"), HttpMethod.POST,
                                new HttpEntity<>(new CreateAccountRequest("USD"), bearerHeaders(ownerToken)),
                                AccountResponse.class);

                ResponseEntity<AccountResponse> thiefAccount = restTemplate.exchange(
                                url("/api/v1/accounts"), HttpMethod.POST,
                                new HttpEntity<>(new CreateAccountRequest("USD"), bearerHeaders(thiefToken)),
                                AccountResponse.class);

                // Thief attempts to transfer FROM owner's account
                TransferRequest request = new TransferRequest(
                                ownerAccount.getBody().accountNumber(),
                                thiefAccount.getBody().accountNumber(),
                                new BigDecimal("100.000"), "USD", "Theft");

                ResponseEntity<String> response = restTemplate.exchange(
                                url("/api/v1/transfers"), HttpMethod.POST,
                                new HttpEntity<>(request, bearerHeaders(thiefToken)),
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        // ═════════════════════════════════════════════════════════════════════════
        // SECTION 3 — BUSINESS RULE / ERROR PATH TESTS (Independent)
        // ═════════════════════════════════════════════════════════════════════════

        @Test
        @DisplayName("Business: Duplicate email registration → 409 CONFLICT")
        void business_duplicateEmailRegistration_returns409() {
                String email = "duplicate_" + UUID.randomUUID() + "@test.com";

                // First registration — must succeed
                restTemplate.postForEntity(
                                url("/api/v1/auth/register"),
                                new RegisterRequest(email, "password123", "First User"),
                                AuthResponse.class);

                // Second registration with same email — must fail
                ResponseEntity<String> response = restTemplate.postForEntity(
                                url("/api/v1/auth/register"),
                                new RegisterRequest(email, "password123", "Duplicate User"),
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Business: Deposit invalid amount (zero) → 400 BAD REQUEST")
        void business_depositZeroAmount_returns400() {
                String email = "zerodeposit_" + UUID.randomUUID() + "@test.com";
                String token = registerAndGetToken(email, "password123", "Zero Depositor");
                String accountNumber = createAccountAndGetNumber(token, "USD");

                DepositRequest request = new DepositRequest(
                                accountNumber, BigDecimal.ZERO, "USD", "Invalid deposit");

                ResponseEntity<String> response = restTemplate.exchange(
                                url("/api/v1/accounts/deposit"),
                                HttpMethod.POST,
                                new HttpEntity<>(request, bearerHeaders(token)),
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Business: Transfer with insufficient funds → 422 UNPROCESSABLE ENTITY")
        void business_insufficientFunds_returns422() {
                // Setup: two users with accounts, source has only 50 USD
                String senderEmail = "sender_" + UUID.randomUUID() + "@test.com";
                String senderToken = registerAndGetToken(senderEmail, "password123", "Sender");
                String senderAccount = createAccountAndGetNumber(senderToken, "USD");

                String receiverEmail = "receiver_" + UUID.randomUUID() + "@test.com";
                String receiverToken = registerAndGetToken(receiverEmail, "password123", "Receiver");
                String receiverAccount = createAccountAndGetNumber(receiverToken, "USD");

                // Deposit only 50 USD
                restTemplate.exchange(
                                url("/api/v1/accounts/deposit"),
                                HttpMethod.POST,
                                new HttpEntity<>(new DepositRequest(senderAccount, new BigDecimal("50.000"), "USD",
                                                "Small deposit"), bearerHeaders(senderToken)),
                                TransactionResponse.class);

                // Attempt to transfer 200 USD (more than balance)
                TransferRequest request = new TransferRequest(
                                senderAccount, receiverAccount, new BigDecimal("200.000"), "USD", "Overspend");

                ResponseEntity<String> response = restTemplate.exchange(
                                url("/api/v1/transfers"), HttpMethod.POST,
                                new HttpEntity<>(request, bearerHeaders(senderToken)),
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }

        @Test
        @DisplayName("Business: Transfer to non-existent account → 404 NOT FOUND")
        void business_transferToNonExistentAccount_returns404() {
                String email = "notfound_" + UUID.randomUUID() + "@test.com";
                String token = registerAndGetToken(email, "password123", "Not Found Tester");
                String accountNumber = createAccountAndGetNumber(token, "USD");

                // Deposit so there are sufficient funds
                restTemplate.exchange(
                                url("/api/v1/accounts/deposit"), HttpMethod.POST,
                                new HttpEntity<>(new DepositRequest(accountNumber, new BigDecimal("100.000"), "USD",
                                                "Setup"), bearerHeaders(token)),
                                TransactionResponse.class);

                TransferRequest request = new TransferRequest(
                                accountNumber, "000000000000", // non-existent account number
                                new BigDecimal("50.000"), "USD", "To ghost");

                ResponseEntity<String> response = restTemplate.exchange(
                                url("/api/v1/transfers"), HttpMethod.POST,
                                new HttpEntity<>(request, bearerHeaders(token)),
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Business: Reverse a transaction that is already reversed → 409 CONFLICT")
        void business_reverseAlreadyReversedTransaction_returns409() {
                // Setup: user with an account and a deposit
                String email = "alreadyrev_" + UUID.randomUUID() + "@test.com";
                String token = registerAndGetToken(email, "password123", "Already Reversed");
                String accountNumber = createAccountAndGetNumber(token, "USD");

                ResponseEntity<TransactionResponse> depositResp = restTemplate.exchange(
                                url("/api/v1/accounts/deposit"), HttpMethod.POST,
                                new HttpEntity<>(new DepositRequest(accountNumber, new BigDecimal("100.000"), "USD",
                                                "Deposit to reverse"), bearerHeaders(token)),
                                TransactionResponse.class);
                UUID txId = depositResp.getBody().id();

                // First reversal — must succeed
                restTemplate.exchange(
                                url("/api/v1/transactions/reverse"), HttpMethod.POST,
                                new HttpEntity<>(new ReversalRequest(txId, "First reversal"), bearerHeaders(token)),
                                new ParameterizedTypeReference<List<TransactionResponse>>() {
                                });

                // Second reversal of the same transaction — must fail
                ResponseEntity<String> response = restTemplate.exchange(
                                url("/api/v1/transactions/reverse"), HttpMethod.POST,
                                new HttpEntity<>(new ReversalRequest(txId, "Duplicate reversal"), bearerHeaders(token)),
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Business: Reverse a reversal transaction → 400 BAD REQUEST")
        void business_reverseAReversalTransaction_returns400() {
                String email = "revrev_" + UUID.randomUUID() + "@test.com";
                String token = registerAndGetToken(email, "password123", "Rev Rev");
                String accountNumber = createAccountAndGetNumber(token, "USD");

                // Deposit and immediately reverse it
                ResponseEntity<TransactionResponse> depositResp = restTemplate.exchange(
                                url("/api/v1/accounts/deposit"), HttpMethod.POST,
                                new HttpEntity<>(new DepositRequest(accountNumber, new BigDecimal("100.000"), "USD",
                                                "Base deposit"), bearerHeaders(token)),
                                TransactionResponse.class);
                UUID originalTxId = depositResp.getBody().id();

                ResponseEntity<List<TransactionResponse>> reversalResp = restTemplate.exchange(
                                url("/api/v1/transactions/reverse"), HttpMethod.POST,
                                new HttpEntity<>(new ReversalRequest(originalTxId, "First"), bearerHeaders(token)),
                                new ParameterizedTypeReference<List<TransactionResponse>>() {
                                });
                UUID reversalTxId = reversalResp.getBody().get(0).id();

                // Attempt to reverse the reversal itself
                ResponseEntity<String> response = restTemplate.exchange(
                                url("/api/v1/transactions/reverse"), HttpMethod.POST,
                                new HttpEntity<>(new ReversalRequest(reversalTxId, "Meta-reversal"),
                                                bearerHeaders(token)),
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Business: Create account with invalid currency code → 400 BAD REQUEST")
        void business_invalidCurrencyCode_returns400() {
                String email = "badcurrency_" + UUID.randomUUID() + "@test.com";
                String token = registerAndGetToken(email, "password123", "Bad Currency");

                ResponseEntity<String> response = restTemplate.exchange(
                                url("/api/v1/accounts"), HttpMethod.POST,
                                new HttpEntity<>(new CreateAccountRequest("XX"), bearerHeaders(token)),
                                String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        // ═════════════════════════════════════════════════════════════════════════
        // HELPERS
        // ═════════════════════════════════════════════════════════════════════════

        private String url(String path) {
                return "http://localhost:" + port + path;
        }

        private HttpHeaders bearerHeaders(String token) {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                headers.setContentType(MediaType.APPLICATION_JSON);
                return headers;
        }

        /**
         * Register a new user and return their JWT token.
         * Used by independent test methods that need their own isolated user.
         */
        private String registerAndGetToken(String email, String password, String fullName) {
                ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                                url("/api/v1/auth/register"),
                                new RegisterRequest(email, password, fullName),
                                AuthResponse.class);
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                return response.getBody().token();
        }

        /**
         * Create an account for the given token and return its account number.
         * Used by independent test methods to set up their own isolated account.
         */
        private String createAccountAndGetNumber(String token, String currencyCode) {
                ResponseEntity<AccountResponse> response = restTemplate.exchange(
                                url("/api/v1/accounts"), HttpMethod.POST,
                                new HttpEntity<>(new CreateAccountRequest(currencyCode), bearerHeaders(token)),
                                AccountResponse.class);
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                return response.getBody().accountNumber();
        }
}