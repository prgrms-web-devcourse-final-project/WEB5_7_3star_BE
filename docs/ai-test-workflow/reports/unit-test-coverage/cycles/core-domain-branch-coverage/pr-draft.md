# PR Draft - Core Domain Branch Coverage

## Summary

- Add unit tests for payment prepare/confirm/cancel, coupon discount/state guards, lesson search branches, user auth/withdraw guards, mapper shape, and request DTO validation.
- Keep the cycle within approved test-only scope: no production code, build/profile, controller slice, repository integration, or Testcontainers changes.
- Apply reviewer feedback to make cancellation refund, coupon lookup, and search count limit assertions more cause-oriented.

## Validation

```text
./gradlew test \
  --tests "com.threestar.trainus.domain.payment.service.PaymentServiceTest" \
  --tests "com.threestar.trainus.domain.coupon.user.service.CouponServiceTest" \
  --tests "com.threestar.trainus.domain.lesson.student.service.StudentLessonServiceTest" \
  --tests "com.threestar.trainus.domain.lesson.student.mapper.LessonSearchMapperTest" \
  --tests "com.threestar.trainus.domain.lesson.student.mapper.LessonApplicationMapperTest" \
  --tests "com.threestar.trainus.domain.user.service.UserServiceTest" \
  --tests "com.threestar.trainus.global.resolver.LoginUserArgumentResolverUnitTest" \
  --tests "com.threestar.trainus.domain.lesson.teacher.dto.LessonCreateRequestDtoTest" \
  --tests "com.threestar.trainus.domain.lesson.teacher.dto.LessonUpdateRequestDtoTest" \
  --tests "com.threestar.trainus.domain.lesson.teacher.dto.ApplicationActionRequestDtoTest" \
  --tests "com.threestar.trainus.global.dto.PageRequestDtoTest" \
  --tests "com.threestar.trainus.domain.payment.dto.PaymentRequestDtoTest" \
  --tests "com.threestar.trainus.domain.payment.dto.ConfirmPaymentRequestDtoTest" \
  --tests "com.threestar.trainus.domain.payment.dto.CancelPaymentRequestDtoTest" \
  --tests "com.threestar.trainus.domain.user.dto.LoginRequestDtoTest" \
  --tests "com.threestar.trainus.domain.user.dto.SignupRequestDtoTest" \
  --tests "com.threestar.trainus.domain.user.dto.PasswordUpdateDtoTest" \
  --tests "com.threestar.trainus.domain.coupon.admin.mapper.AdminCouponMapperTest"
```

Result: `BUILD SUCCESSFUL`

## Notes

- Full `./gradlew test` was not used as the gate for this unit-only cycle, per approved plan.
- Integration-only paths remain outside this PR draft scope.
