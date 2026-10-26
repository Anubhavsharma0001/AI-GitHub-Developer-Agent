package com.anubhav.Backend.dto;




    public class ApiResponse {

        private String name;
        private String status;
        private String message;

        public ApiResponse(String name, String status, String message) {
            this.name = name;
            this.status = status;
            this.message = message;
        }

        public String getName() {
            return name;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

