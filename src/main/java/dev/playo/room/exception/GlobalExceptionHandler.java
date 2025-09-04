/*
 * Copyright 2019-2025 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.playo.room.exception;

import jakarta.annotation.Nonnull;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.StringJoiner;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Contains the global handlers for exceptions.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  private final URI problemInstance;

  @Autowired
  public GlobalExceptionHandler(@Value("${info.app.version:dev-local}") String applicationVersion) {
    this.problemInstance = URI.create("room-management-" + applicationVersion);
  }

  /**
   * Formats the request URI of the given request to optionally include the query string.
   *
   * @param request the request to format the URI of.
   * @return the formatted URI of the given request.
   */
  private static @Nonnull String formatRequestUri(@Nonnull HttpServletRequest request) {
    var requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    if (requestUri == null) {
      requestUri = request.getRequestURI();
    }

    var queryString = request.getQueryString();
    if (queryString != null && !queryString.isBlank()) {
      requestUri += '?' + queryString;
    }

    return requestUri;
  }

  /**
   * Prettifies the path of a constraint violation.
   *
   * @param propertyPath the path to be prettified.
   * @return the prettified path of the constraint violation.
   */
  private static @Nonnull String prettifyPropertyPath(@Nonnull Path propertyPath) {
    // construct a joiner and use an empty string as the default value for further checks
    var joiner = new StringJoiner(".");
    joiner.setEmptyValue("");

    for (var node : propertyPath) {
      // skip some types of node
      var kind = node.getKind();
      if (kind == ElementKind.METHOD
          || kind == ElementKind.CONSTRUCTOR
          || kind == ElementKind.CROSS_PARAMETER
          || kind == ElementKind.RETURN_VALUE) {
        continue;
      }

      // append the node if it helps for the path understanding
      var nodeAsString = node.toString();
      if (!nodeAsString.isBlank()) {
        joiner.add(nodeAsString);
      }
    }

    // return the default path in case no elements were added to the string joiner
    // note that this only works due to the fact that the empty value of the joiner is an empty string, as the joiner
    // returns the length of the empty value in case nothing was added
    if (joiner.length() == 0) {
      return propertyPath.toString();
    } else {
      return joiner.toString();
    }
  }

  /**
   * Builds the base problem detail using the given http response code.
   *
   * @param request the request that caused the problem.
   * @param status  the status to return for the problem.
   * @return the base problem detail instant for further configuration.
   */
  private @Nonnull ProblemDetail buildBaseProblemDetail(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpStatus status
  ) {
    var problemDetail = ProblemDetail.forStatus(status);
    problemDetail.setInstance(this.problemInstance);
    problemDetail.setProperty("request-uri", formatRequestUri(request));
    problemDetail.setProperty("timestamp", OffsetDateTime.now(ZoneOffset.UTC));
    return problemDetail;
  }

  /**
   * Fallback exception handler for all exceptions that are not handled by any other handler.
   */
  @ExceptionHandler(Exception.class)
  public @Nonnull ProblemDetail handleUnhandledExceptions(
      @Nonnull Exception exception,
      @Nonnull HttpServletRequest request
  ) {
    var problemDetail = this.buildBaseProblemDetail(request, HttpStatus.INTERNAL_SERVER_ERROR);
    if (exception instanceof ErrorResponse errorResponse) {
      // exception that has some problem information already set
      var providedProblem = errorResponse.getBody();
      problemDetail.setTitle(providedProblem.getTitle());
      problemDetail.setDetail(providedProblem.getDetail());
      problemDetail.setStatus(providedProblem.getStatus());
    } else {
      // some other exception that was thrown
      problemDetail.setTitle("Internal Server Error");
      problemDetail.setDetail("An internal error occurred while processing the request");
    }

    return problemDetail;
  }

  /**
   * Handles the case when a requested resource does not exist and resolves it to a 404 response.
   */
  @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
  public @Nonnull ProblemDetail handleUnknownResource(@Nonnull HttpServletRequest request) {
    var problemDetail = this.buildBaseProblemDetail(request, HttpStatus.NOT_FOUND);
    problemDetail.setTitle("Resource not found");
    problemDetail.setDetail("The requested resource does not exist");
    return problemDetail;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public @Nonnull ProblemDetail handleMethodArgumentNotValidException(
      @Nonnull MethodArgumentNotValidException exception,
      @Nonnull HttpServletRequest request
  ) {
    var problemDetail = this.buildBaseProblemDetail(request, HttpStatus.BAD_REQUEST);
    problemDetail.setTitle("Constraint Violation");
    problemDetail.setDetail("Parameter(s) did not pass constraint validation");
    // might need some love for better formatting
    problemDetail.setProperty("fieldViolations", exception.getBindingResult().getFieldErrors());
    problemDetail.setProperty("globalViolations", exception.getBindingResult().getGlobalErrors());

    return problemDetail;
  }

  @ExceptionHandler(MultipartException.class)
  public @Nonnull ProblemDetail handleMultipartException(@Nonnull HttpServletRequest request) {
    var problemDetail = this.buildBaseProblemDetail(request, HttpStatus.BAD_REQUEST);
    problemDetail.setTitle("Multipart Exception");
    problemDetail.setDetail("The request did not contain a valid multipart body");
    return problemDetail;
  }

  /**
   * Called when a problem exception is caught.
   */
  @ExceptionHandler(GeneralProblemException.class)
  public @Nonnull ProblemDetail handleProblemException(
      @Nonnull HttpServletRequest request,
      @Nonnull GeneralProblemException exception
  ) {
    var problemDetail = this.buildBaseProblemDetail(request, exception.getStatus());
    problemDetail.setTitle(exception.getStatus().getReasonPhrase());
    problemDetail.setDetail(exception.getDescription());
    return problemDetail;
  }

  /**
   * Handles the case where a type cannot be deserialized from a given input.
   */
  @ExceptionHandler(TypeMismatchException.class)
  public @Nonnull ProblemDetail handleTypeMismatchException(
      @Nonnull TypeMismatchException exception,
      @Nonnull HttpServletRequest request
  ) {
    var prop = exception.getPropertyName();
    var detail = (prop != null ? prop + ": " : "") + "cannot convert from input '" + exception.getValue() + "'";

    var problemDetail = this.buildBaseProblemDetail(request, HttpStatus.BAD_REQUEST);
    problemDetail.setTitle("Type Mismatch");
    problemDetail.setDetail(detail);
    return problemDetail;
  }

  /**
   * Handler for constraint violations of requests.
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public @Nonnull ProblemDetail handleConstraintViolationException(
      @Nonnull ConstraintViolationException exception,
      @Nonnull HttpServletRequest request
  ) {
    var problemDetail = this.buildBaseProblemDetail(request, HttpStatus.BAD_REQUEST);
    problemDetail.setTitle("Constraint Violation");
    problemDetail.setDetail("Parameter(s) did not pass constraint validation");

    var violations = exception.getConstraintViolations();
    if (violations != null && !violations.isEmpty()) {
      var formattedViolations = violations.stream()
          .filter(Objects::nonNull)
          .map(cv -> prettifyPropertyPath(cv.getPropertyPath()) + ": " + cv.getMessage())
          .toList();
      problemDetail.setProperty("violations", formattedViolations);
    }

    return problemDetail;
  }
}
