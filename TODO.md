# Easebuzz Payment Integration Fix Plan

## Task
Fix the Easebuzz initiateLink API integration to resolve "Parameter validation failed" error.

## Issues Identified
1. API endpoint might be incorrect (should be /payment/initiateLink)
2. Hash format might not match Easebuzz's exact requirements
3. Need better logging for debugging
4. Need to format amount to 2 decimal places

## TODO List

### 1. Update EasebuzzConfig.java
- [ ] Change initiate URL from "/payment/initiate" to "/payment/initiateLink"

### 2. Update EasebuzzHashUtil.java
- [ ] Fix hash format to use 5 empty fields between udf1 and salt: key|txnid|amount|productinfo|firstname|email|udf1|||||||||salt

### 3. Update PaymentService.java
- [ ] Add detailed logging of request map before sending
- [ ] Log hash input string
- [ ] Log raw response body from Easebuzz
- [ ] Format amount to 2 decimal places
- [ ] Properly parse response with status, data, error_desc fields
- [ ] Throw GatewayException with error_desc when status != 1

### 4. Create/update DTOs if needed
- [ ] Ensure EasebuzzRequest DTO has all required fields
- [ ] Ensure EasebuzzResponse DTO parses status, data, error_desc correctly
