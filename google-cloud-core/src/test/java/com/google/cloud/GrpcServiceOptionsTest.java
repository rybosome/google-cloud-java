/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.GrpcServiceOptions.DefaultExecutorFactory;
import com.google.cloud.GrpcServiceOptions.ExecutorFactory;
import com.google.cloud.spi.ServiceRpcFactory;

import org.easymock.EasyMock;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

public class GrpcServiceOptionsTest {

  private static final ExecutorFactory MOCK_EXECUTOR_FACTORY =
      EasyMock.createMock(ExecutorFactory.class);
  private static final TestGrpcServiceOptions OPTIONS = TestGrpcServiceOptions.newBuilder()
      .setProjectId("project-id")
      .setInitialTimeout(1234)
      .setTimeoutMultiplier(1.6)
      .setMaxTimeout(5678)
      .setExecutorFactory(MOCK_EXECUTOR_FACTORY)
      .build();
  private static final TestGrpcServiceOptions DEFAULT_OPTIONS =
      TestGrpcServiceOptions.newBuilder().setProjectId("project-id").build();
  private static final TestGrpcServiceOptions OPTIONS_COPY = OPTIONS.toBuilder().build();

  private interface TestService extends Service<TestGrpcServiceOptions> {}

  private static class TestServiceImpl
      extends BaseService<TestGrpcServiceOptions> implements TestService {
    private TestServiceImpl(TestGrpcServiceOptions options) {
      super(options);
    }
  }

  private interface TestServiceFactory extends ServiceFactory<TestService, TestGrpcServiceOptions> {
  }

  private static class DefaultTestServiceFactory implements TestServiceFactory {
    private static final TestServiceFactory INSTANCE = new DefaultTestServiceFactory();

    @Override
    public TestService create(TestGrpcServiceOptions options) {
      return new TestServiceImpl(options);
    }
  }

  private interface TestServiceRpcFactory
      extends ServiceRpcFactory<TestServiceRpc, TestGrpcServiceOptions> {}

  private static class DefaultTestServiceRpcFactory implements TestServiceRpcFactory {
    private static final TestServiceRpcFactory INSTANCE = new DefaultTestServiceRpcFactory();

    @Override
    public TestServiceRpc create(TestGrpcServiceOptions options) {
      return new DefaultTestServiceRpc(options);
    }
  }

  private interface TestServiceRpc {}

  private static class DefaultTestServiceRpc implements TestServiceRpc {
    DefaultTestServiceRpc(TestGrpcServiceOptions options) {}
  }

  private static class TestGrpcServiceOptions
      extends GrpcServiceOptions<TestService, TestServiceRpc, TestGrpcServiceOptions> {
    private static class Builder
        extends GrpcServiceOptions.Builder<TestService, TestServiceRpc, TestGrpcServiceOptions,
        Builder> {
      private Builder() {}

      private Builder(TestGrpcServiceOptions options) {
        super(options);
      }

      @Override
      protected TestGrpcServiceOptions build() {
        return new TestGrpcServiceOptions(this);
      }
    }

    private TestGrpcServiceOptions(Builder builder) {
      super(TestServiceFactory.class, TestServiceRpcFactory.class, builder);
    }

    @Override
    protected TestServiceFactory getDefaultServiceFactory() {
      return DefaultTestServiceFactory.INSTANCE;
    }

    @Override
    protected TestServiceRpcFactory getDefaultRpcFactory() {
      return DefaultTestServiceRpcFactory.INSTANCE;
    }

    @Override
    protected Set<String> getScopes() {
      return null;
    }

    @Override
    public Builder toBuilder() {
      return new Builder(this);
    }

    private static Builder newBuilder() {
      return new Builder();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof TestGrpcServiceOptions && baseEquals((TestGrpcServiceOptions) obj);
    }

    @Override
    public int hashCode() {
      return baseHashCode();
    }
  }

  @Test
  public void testBuilder() {
    assertEquals(1234, OPTIONS.getInitialTimeout());
    assertEquals(1.6, OPTIONS.getTimeoutMultiplier(), 0.0);
    assertEquals(5678, OPTIONS.getMaxTimeout());
    assertSame(MOCK_EXECUTOR_FACTORY, OPTIONS.getExecutorFactory());
    assertEquals(20000, DEFAULT_OPTIONS.getInitialTimeout());
    assertEquals(1.5, DEFAULT_OPTIONS.getTimeoutMultiplier(), 0.0);
    assertEquals(100000, DEFAULT_OPTIONS.getMaxTimeout());
    assertTrue(DEFAULT_OPTIONS.getExecutorFactory() instanceof DefaultExecutorFactory);
  }

  @Test
  public void testBuilderError() {
    try {
      TestGrpcServiceOptions.newBuilder().setInitialTimeout(0);
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException ex) {
      assertEquals("Initial timeout must be > 0", ex.getMessage());
    }
    try {
      TestGrpcServiceOptions.newBuilder().setInitialTimeout(-1);
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException ex) {
      assertEquals("Initial timeout must be > 0", ex.getMessage());
    }
    try {
      TestGrpcServiceOptions.newBuilder().setTimeoutMultiplier(0.9);
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException ex) {
      assertEquals("Timeout multiplier must be >= 1", ex.getMessage());
    }
  }

  @Test
  public void testBuilderInvalidMaxTimeout() {
    TestGrpcServiceOptions options = TestGrpcServiceOptions.newBuilder()
        .setProjectId("project-id")
        .setInitialTimeout(1234)
        .setTimeoutMultiplier(1.6)
        .setMaxTimeout(123)
        .build();
    assertEquals(1234, options.getInitialTimeout());
    assertEquals(1.6, options.getTimeoutMultiplier(), 0.0);
    assertEquals(1234, options.getMaxTimeout());
  }

  @Test
  public void testBaseEquals() {
    assertEquals(OPTIONS, OPTIONS_COPY);
    assertNotEquals(DEFAULT_OPTIONS, OPTIONS);
    TestGrpcServiceOptions options = OPTIONS.toBuilder()
        .setExecutorFactory(new DefaultExecutorFactory())
        .build();
    assertNotEquals(OPTIONS, options);
  }

  @Test
  public void testBaseHashCode() {
    assertEquals(OPTIONS.hashCode(), OPTIONS_COPY.hashCode());
    assertNotEquals(DEFAULT_OPTIONS.hashCode(), OPTIONS.hashCode());
    TestGrpcServiceOptions options = OPTIONS.toBuilder()
        .setExecutorFactory(new DefaultExecutorFactory())
        .build();
    assertNotEquals(OPTIONS.hashCode(), options.hashCode());
  }

  @Test
  public void testDefaultExecutorFactory() {
    ExecutorFactory<ScheduledExecutorService> executorFactory = new DefaultExecutorFactory();
    ScheduledExecutorService executorService = executorFactory.get();
    assertSame(executorService, executorFactory.get());
  }
}
