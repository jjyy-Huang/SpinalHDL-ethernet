// Generator : SpinalHDL v1.7.1    git head : 0444bb76ab1d6e19f0ec46bc03c4769776deb7d5
// Component : AXI4StreamInsertTopLevel
// Git hash  : 981f39d88125b800b2e94bdc93311e20bcedab86
// 
// @Author : Jinyuan Huang (Jerry) jjyy.huang@gmail.com
// @Create : Wed Oct 26 14:07:53 UTC 2022

`timescale 1ns/1ps

module AXI4StreamInsertTopLevel (
  input               dataIn_AXIS_port_valid,
  output              dataIn_AXIS_port_ready,
  input      [31:0]   dataIn_AXIS_port_payload_data,
  input      [3:0]    dataIn_AXIS_port_payload_keep,
  input               dataIn_AXIS_port_payload_last,
  input               headerIn_AXIS_port_valid,
  output              headerIn_AXIS_port_ready,
  input      [31:0]   headerIn_AXIS_port_payload_data,
  input      [3:0]    headerIn_AXIS_port_payload_keep,
  input      [3:0]    headerIn_AXIS_port_payload_user,
  output              dataOut_AXIS_port_valid,
  input               dataOut_AXIS_port_ready,
  output     [31:0]   dataOut_AXIS_port_payload_data,
  output reg [3:0]    dataOut_AXIS_port_payload_keep,
  output reg          dataOut_AXIS_port_payload_last,
  input               clk,
  input               reset
);

  wire       [3:0]    _zz_remainCnt_8;
  wire       [2:0]    _zz_remainCnt_9;
  reg        [2:0]    _zz_remainCnt_10;
  wire       [2:0]    _zz_remainCnt_11;
  reg        [2:0]    _zz_remainCnt_12;
  wire       [2:0]    _zz_remainCnt_13;
  wire       [0:0]    _zz_remainCnt_14;
  wire       [2:0]    _zz_switch_AXI4StreamInsertTopLevel_l136;
  wire       [2:0]    _zz_switch_AXI4StreamInsertTopLevel_l136_1;
  wire       [2:0]    _zz_switch_AXI4StreamInsertTopLevel_l136_2;
  wire       [2:0]    _zz_switch_AXI4StreamInsertTopLevel_l136_3;
  wire       [3:0]    _zz_when_AXI4StreamInsertTopLevel_l190;
  reg                 getHeader;
  reg                 receiveComplete;
  wire                headerIn_AXIS_port_fire;
  reg        [3:0]    selectedMode;
  wire                headerIn_AXIS_port_fire_1;
  reg        [3:0]    remainCnt;
  wire       [2:0]    _zz_remainCnt;
  wire       [2:0]    _zz_remainCnt_1;
  wire       [2:0]    _zz_remainCnt_2;
  wire       [2:0]    _zz_remainCnt_3;
  wire       [2:0]    _zz_remainCnt_4;
  wire       [2:0]    _zz_remainCnt_5;
  wire       [2:0]    _zz_remainCnt_6;
  wire       [2:0]    _zz_remainCnt_7;
  reg        [1:0]    delayCycle;
  reg        [3:0]    lastKeep;
  reg        [3:0]    delayCnt;
  wire                when_AXI4StreamInsertTopLevel_l60;
  wire                when_AXI4StreamInsertTopLevel_l62;
  reg        [7:0]    dataCache_0_0;
  reg                 dataCache_0_1;
  reg        [7:0]    dataCache_1_0;
  reg                 dataCache_1_1;
  reg        [7:0]    dataCache_2_0;
  reg                 dataCache_2_1;
  reg        [7:0]    dataCache_3_0;
  reg                 dataCache_3_1;
  reg        [7:0]    issueCache_0;
  reg        [7:0]    issueCache_1;
  reg        [7:0]    issueCache_2;
  reg        [7:0]    issueCache_3;
  wire                issueCacheReady;
  wire       [7:0]    bypassData_0_0;
  wire                bypassData_0_1;
  wire       [7:0]    bypassData_1_0;
  wire                bypassData_1_1;
  wire       [7:0]    bypassData_2_0;
  wire                bypassData_2_1;
  wire       [7:0]    bypassData_3_0;
  wire                bypassData_3_1;
  wire                dataIn_AXIS_port_fire;
  reg        [7:0]    mappedData_0_0;
  reg                 mappedData_0_1;
  reg        [7:0]    mappedData_1_0;
  reg                 mappedData_1_1;
  reg        [7:0]    mappedData_2_0;
  reg                 mappedData_2_1;
  reg        [7:0]    mappedData_3_0;
  reg                 mappedData_3_1;
  reg        [7:0]    mappedHeader_0_0;
  reg                 mappedHeader_0_1;
  reg        [7:0]    mappedHeader_1_0;
  reg                 mappedHeader_1_1;
  reg        [7:0]    mappedHeader_2_0;
  reg                 mappedHeader_2_1;
  reg        [7:0]    mappedHeader_3_0;
  reg                 mappedHeader_3_1;
  wire       [2:0]    switch_AXI4StreamInsertTopLevel_l136;
  wire       [2:0]    switch_AXI4StreamInsertTopLevel_l136_1;
  wire       [2:0]    switch_AXI4StreamInsertTopLevel_l136_2;
  wire       [2:0]    switch_AXI4StreamInsertTopLevel_l136_3;
  wire                when_AXI4StreamInsertTopLevel_l151;
  wire                dataIn_AXIS_port_fire_1;
  wire                when_AXI4StreamInsertTopLevel_l159;
  wire                when_AXI4StreamInsertTopLevel_l164;
  wire                when_AXI4StreamInsertTopLevel_l151_1;
  wire                dataIn_AXIS_port_fire_2;
  wire                when_AXI4StreamInsertTopLevel_l159_1;
  wire                when_AXI4StreamInsertTopLevel_l164_1;
  wire                when_AXI4StreamInsertTopLevel_l151_2;
  wire                dataIn_AXIS_port_fire_3;
  wire                when_AXI4StreamInsertTopLevel_l159_2;
  wire                when_AXI4StreamInsertTopLevel_l164_2;
  wire                when_AXI4StreamInsertTopLevel_l151_3;
  wire                dataIn_AXIS_port_fire_4;
  wire                when_AXI4StreamInsertTopLevel_l159_3;
  wire                when_AXI4StreamInsertTopLevel_l164_3;
  reg                 issueCacheLoaded;
  wire       [31:0]   combineData;
  wire                dataIn_AXIS_port_fire_5;
  wire                when_AXI4StreamInsertTopLevel_l180;
  wire                when_AXI4StreamInsertTopLevel_l182;
  wire                when_AXI4StreamInsertTopLevel_l190;

  assign _zz_remainCnt_9 = (_zz_remainCnt_10 + _zz_remainCnt_12);
  assign _zz_remainCnt_8 = {1'd0, _zz_remainCnt_9};
  assign _zz_remainCnt_14 = dataIn_AXIS_port_payload_keep[3];
  assign _zz_remainCnt_13 = {2'd0, _zz_remainCnt_14};
  assign _zz_switch_AXI4StreamInsertTopLevel_l136 = headerIn_AXIS_port_payload_user[2:0];
  assign _zz_switch_AXI4StreamInsertTopLevel_l136_1 = headerIn_AXIS_port_payload_user[2:0];
  assign _zz_switch_AXI4StreamInsertTopLevel_l136_2 = headerIn_AXIS_port_payload_user[2:0];
  assign _zz_switch_AXI4StreamInsertTopLevel_l136_3 = headerIn_AXIS_port_payload_user[2:0];
  assign _zz_when_AXI4StreamInsertTopLevel_l190 = {2'd0, delayCycle};
  assign _zz_remainCnt_11 = {dataIn_AXIS_port_payload_keep[2],{dataIn_AXIS_port_payload_keep[1],dataIn_AXIS_port_payload_keep[0]}};
  always @(*) begin
    case(_zz_remainCnt_11)
      3'b000 : _zz_remainCnt_10 = _zz_remainCnt;
      3'b001 : _zz_remainCnt_10 = _zz_remainCnt_1;
      3'b010 : _zz_remainCnt_10 = _zz_remainCnt_2;
      3'b011 : _zz_remainCnt_10 = _zz_remainCnt_3;
      3'b100 : _zz_remainCnt_10 = _zz_remainCnt_4;
      3'b101 : _zz_remainCnt_10 = _zz_remainCnt_5;
      3'b110 : _zz_remainCnt_10 = _zz_remainCnt_6;
      default : _zz_remainCnt_10 = _zz_remainCnt_7;
    endcase
  end

  always @(*) begin
    case(_zz_remainCnt_13)
      3'b000 : _zz_remainCnt_12 = _zz_remainCnt;
      3'b001 : _zz_remainCnt_12 = _zz_remainCnt_1;
      3'b010 : _zz_remainCnt_12 = _zz_remainCnt_2;
      3'b011 : _zz_remainCnt_12 = _zz_remainCnt_3;
      3'b100 : _zz_remainCnt_12 = _zz_remainCnt_4;
      3'b101 : _zz_remainCnt_12 = _zz_remainCnt_5;
      3'b110 : _zz_remainCnt_12 = _zz_remainCnt_6;
      default : _zz_remainCnt_12 = _zz_remainCnt_7;
    endcase
  end

  assign headerIn_AXIS_port_fire = (headerIn_AXIS_port_valid && headerIn_AXIS_port_ready);
  assign headerIn_AXIS_port_fire_1 = (headerIn_AXIS_port_valid && headerIn_AXIS_port_ready);
  assign _zz_remainCnt = 3'b000;
  assign _zz_remainCnt_1 = 3'b001;
  assign _zz_remainCnt_2 = 3'b001;
  assign _zz_remainCnt_3 = 3'b010;
  assign _zz_remainCnt_4 = 3'b001;
  assign _zz_remainCnt_5 = 3'b010;
  assign _zz_remainCnt_6 = 3'b010;
  assign _zz_remainCnt_7 = 3'b011;
  assign when_AXI4StreamInsertTopLevel_l60 = 1'b0;
  assign when_AXI4StreamInsertTopLevel_l62 = (receiveComplete && dataOut_AXIS_port_ready);
  always @(*) begin
    case(remainCnt)
      4'b0001 : begin
        delayCycle = 2'b00;
      end
      4'b0010 : begin
        delayCycle = 2'b00;
      end
      4'b0011 : begin
        delayCycle = 2'b00;
      end
      4'b0100 : begin
        delayCycle = 2'b00;
      end
      4'b0101 : begin
        delayCycle = 2'b01;
      end
      4'b0110 : begin
        delayCycle = 2'b01;
      end
      4'b0111 : begin
        delayCycle = 2'b01;
      end
      4'b1000 : begin
        delayCycle = 2'b01;
      end
      default : begin
        delayCycle = 2'b00;
      end
    endcase
  end

  always @(*) begin
    case(remainCnt)
      4'b0001 : begin
        lastKeep = 4'b1000;
      end
      4'b0010 : begin
        lastKeep = 4'b1100;
      end
      4'b0011 : begin
        lastKeep = 4'b1110;
      end
      4'b0100 : begin
        lastKeep = 4'b1111;
      end
      4'b0101 : begin
        lastKeep = 4'b1000;
      end
      4'b0110 : begin
        lastKeep = 4'b1100;
      end
      4'b0111 : begin
        lastKeep = 4'b1110;
      end
      4'b1000 : begin
        lastKeep = 4'b1111;
      end
      default : begin
        lastKeep = 4'b1111;
      end
    endcase
  end

  assign issueCacheReady = dataOut_AXIS_port_ready;
  assign dataIn_AXIS_port_ready = (issueCacheReady && getHeader);
  assign headerIn_AXIS_port_ready = (issueCacheReady && (! getHeader));
  assign bypassData_0_0 = dataIn_AXIS_port_payload_data[7 : 0];
  assign bypassData_0_1 = dataIn_AXIS_port_payload_keep[0];
  assign bypassData_1_0 = dataIn_AXIS_port_payload_data[15 : 8];
  assign bypassData_1_1 = dataIn_AXIS_port_payload_keep[1];
  assign bypassData_2_0 = dataIn_AXIS_port_payload_data[23 : 16];
  assign bypassData_2_1 = dataIn_AXIS_port_payload_keep[2];
  assign bypassData_3_0 = dataIn_AXIS_port_payload_data[31 : 24];
  assign bypassData_3_1 = dataIn_AXIS_port_payload_keep[3];
  assign dataIn_AXIS_port_fire = (dataIn_AXIS_port_valid && dataIn_AXIS_port_ready);
  always @(*) begin
    case(selectedMode)
      4'b0000 : begin
        mappedData_0_0 = bypassData_3_0;
      end
      4'b0001 : begin
        mappedData_0_0 = dataCache_0_0;
      end
      4'b0010 : begin
        mappedData_0_0 = dataCache_1_0;
      end
      4'b0011 : begin
        mappedData_0_0 = dataCache_2_0;
      end
      4'b0100 : begin
        mappedData_0_0 = dataCache_3_0;
      end
      default : begin
        mappedData_0_0 = 8'h0;
      end
    endcase
  end

  always @(*) begin
    case(selectedMode)
      4'b0000 : begin
        mappedData_0_1 = bypassData_3_1;
      end
      4'b0001 : begin
        mappedData_0_1 = dataCache_0_1;
      end
      4'b0010 : begin
        mappedData_0_1 = dataCache_1_1;
      end
      4'b0011 : begin
        mappedData_0_1 = dataCache_2_1;
      end
      4'b0100 : begin
        mappedData_0_1 = dataCache_3_1;
      end
      default : begin
        mappedData_0_1 = 1'b0;
      end
    endcase
  end

  always @(*) begin
    case(selectedMode)
      4'b0000 : begin
        mappedData_1_0 = bypassData_2_0;
      end
      4'b0001 : begin
        mappedData_1_0 = bypassData_3_0;
      end
      4'b0010 : begin
        mappedData_1_0 = dataCache_0_0;
      end
      4'b0011 : begin
        mappedData_1_0 = dataCache_1_0;
      end
      4'b0100 : begin
        mappedData_1_0 = dataCache_2_0;
      end
      default : begin
        mappedData_1_0 = 8'h0;
      end
    endcase
  end

  always @(*) begin
    case(selectedMode)
      4'b0000 : begin
        mappedData_1_1 = bypassData_2_1;
      end
      4'b0001 : begin
        mappedData_1_1 = bypassData_3_1;
      end
      4'b0010 : begin
        mappedData_1_1 = dataCache_0_1;
      end
      4'b0011 : begin
        mappedData_1_1 = dataCache_1_1;
      end
      4'b0100 : begin
        mappedData_1_1 = dataCache_2_1;
      end
      default : begin
        mappedData_1_1 = 1'b0;
      end
    endcase
  end

  always @(*) begin
    case(selectedMode)
      4'b0000 : begin
        mappedData_2_0 = bypassData_1_0;
      end
      4'b0001 : begin
        mappedData_2_0 = bypassData_2_0;
      end
      4'b0010 : begin
        mappedData_2_0 = bypassData_3_0;
      end
      4'b0011 : begin
        mappedData_2_0 = dataCache_0_0;
      end
      4'b0100 : begin
        mappedData_2_0 = dataCache_1_0;
      end
      default : begin
        mappedData_2_0 = 8'h0;
      end
    endcase
  end

  always @(*) begin
    case(selectedMode)
      4'b0000 : begin
        mappedData_2_1 = bypassData_1_1;
      end
      4'b0001 : begin
        mappedData_2_1 = bypassData_2_1;
      end
      4'b0010 : begin
        mappedData_2_1 = bypassData_3_1;
      end
      4'b0011 : begin
        mappedData_2_1 = dataCache_0_1;
      end
      4'b0100 : begin
        mappedData_2_1 = dataCache_1_1;
      end
      default : begin
        mappedData_2_1 = 1'b0;
      end
    endcase
  end

  always @(*) begin
    case(selectedMode)
      4'b0000 : begin
        mappedData_3_0 = bypassData_0_0;
      end
      4'b0001 : begin
        mappedData_3_0 = bypassData_1_0;
      end
      4'b0010 : begin
        mappedData_3_0 = bypassData_2_0;
      end
      4'b0011 : begin
        mappedData_3_0 = bypassData_3_0;
      end
      4'b0100 : begin
        mappedData_3_0 = dataCache_0_0;
      end
      default : begin
        mappedData_3_0 = 8'h0;
      end
    endcase
  end

  always @(*) begin
    case(selectedMode)
      4'b0000 : begin
        mappedData_3_1 = bypassData_0_1;
      end
      4'b0001 : begin
        mappedData_3_1 = bypassData_1_1;
      end
      4'b0010 : begin
        mappedData_3_1 = bypassData_2_1;
      end
      4'b0011 : begin
        mappedData_3_1 = bypassData_3_1;
      end
      4'b0100 : begin
        mappedData_3_1 = dataCache_0_1;
      end
      default : begin
        mappedData_3_1 = 1'b0;
      end
    endcase
  end

  assign switch_AXI4StreamInsertTopLevel_l136 = _zz_switch_AXI4StreamInsertTopLevel_l136;
  always @(*) begin
    case(switch_AXI4StreamInsertTopLevel_l136)
      3'b100 : begin
        mappedHeader_0_0 = headerIn_AXIS_port_payload_data[31 : 24];
      end
      3'b011 : begin
        mappedHeader_0_0 = headerIn_AXIS_port_payload_data[23 : 16];
      end
      3'b010 : begin
        mappedHeader_0_0 = headerIn_AXIS_port_payload_data[15 : 8];
      end
      3'b001 : begin
        mappedHeader_0_0 = headerIn_AXIS_port_payload_data[7 : 0];
      end
      default : begin
        mappedHeader_0_0 = 8'h0;
      end
    endcase
  end

  always @(*) begin
    case(switch_AXI4StreamInsertTopLevel_l136)
      3'b100 : begin
        mappedHeader_0_1 = 1'b1;
      end
      3'b011 : begin
        mappedHeader_0_1 = 1'b1;
      end
      3'b010 : begin
        mappedHeader_0_1 = 1'b1;
      end
      3'b001 : begin
        mappedHeader_0_1 = 1'b1;
      end
      default : begin
        mappedHeader_0_1 = 1'b0;
      end
    endcase
  end

  assign switch_AXI4StreamInsertTopLevel_l136_1 = _zz_switch_AXI4StreamInsertTopLevel_l136_1;
  always @(*) begin
    case(switch_AXI4StreamInsertTopLevel_l136_1)
      3'b100 : begin
        mappedHeader_1_0 = headerIn_AXIS_port_payload_data[23 : 16];
      end
      3'b011 : begin
        mappedHeader_1_0 = headerIn_AXIS_port_payload_data[15 : 8];
      end
      3'b010 : begin
        mappedHeader_1_0 = headerIn_AXIS_port_payload_data[7 : 0];
      end
      default : begin
        mappedHeader_1_0 = 8'h0;
      end
    endcase
  end

  always @(*) begin
    case(switch_AXI4StreamInsertTopLevel_l136_1)
      3'b100 : begin
        mappedHeader_1_1 = 1'b1;
      end
      3'b011 : begin
        mappedHeader_1_1 = 1'b1;
      end
      3'b010 : begin
        mappedHeader_1_1 = 1'b1;
      end
      default : begin
        mappedHeader_1_1 = 1'b0;
      end
    endcase
  end

  assign switch_AXI4StreamInsertTopLevel_l136_2 = _zz_switch_AXI4StreamInsertTopLevel_l136_2;
  always @(*) begin
    case(switch_AXI4StreamInsertTopLevel_l136_2)
      3'b100 : begin
        mappedHeader_2_0 = headerIn_AXIS_port_payload_data[15 : 8];
      end
      3'b011 : begin
        mappedHeader_2_0 = headerIn_AXIS_port_payload_data[7 : 0];
      end
      default : begin
        mappedHeader_2_0 = 8'h0;
      end
    endcase
  end

  always @(*) begin
    case(switch_AXI4StreamInsertTopLevel_l136_2)
      3'b100 : begin
        mappedHeader_2_1 = 1'b1;
      end
      3'b011 : begin
        mappedHeader_2_1 = 1'b1;
      end
      default : begin
        mappedHeader_2_1 = 1'b0;
      end
    endcase
  end

  assign switch_AXI4StreamInsertTopLevel_l136_3 = _zz_switch_AXI4StreamInsertTopLevel_l136_3;
  always @(*) begin
    case(switch_AXI4StreamInsertTopLevel_l136_3)
      3'b100 : begin
        mappedHeader_3_0 = headerIn_AXIS_port_payload_data[7 : 0];
      end
      default : begin
        mappedHeader_3_0 = 8'h0;
      end
    endcase
  end

  always @(*) begin
    case(switch_AXI4StreamInsertTopLevel_l136_3)
      3'b100 : begin
        mappedHeader_3_1 = 1'b1;
      end
      default : begin
        mappedHeader_3_1 = 1'b0;
      end
    endcase
  end

  assign when_AXI4StreamInsertTopLevel_l151 = (! getHeader);
  assign dataIn_AXIS_port_fire_1 = (dataIn_AXIS_port_valid && dataIn_AXIS_port_ready);
  assign when_AXI4StreamInsertTopLevel_l159 = (mappedData_0_1 && issueCacheReady);
  assign when_AXI4StreamInsertTopLevel_l164 = (receiveComplete && issueCacheReady);
  assign when_AXI4StreamInsertTopLevel_l151_1 = (! getHeader);
  assign dataIn_AXIS_port_fire_2 = (dataIn_AXIS_port_valid && dataIn_AXIS_port_ready);
  assign when_AXI4StreamInsertTopLevel_l159_1 = (mappedData_1_1 && issueCacheReady);
  assign when_AXI4StreamInsertTopLevel_l164_1 = (receiveComplete && issueCacheReady);
  assign when_AXI4StreamInsertTopLevel_l151_2 = (! getHeader);
  assign dataIn_AXIS_port_fire_3 = (dataIn_AXIS_port_valid && dataIn_AXIS_port_ready);
  assign when_AXI4StreamInsertTopLevel_l159_2 = (mappedData_2_1 && issueCacheReady);
  assign when_AXI4StreamInsertTopLevel_l164_2 = (receiveComplete && issueCacheReady);
  assign when_AXI4StreamInsertTopLevel_l151_3 = (! getHeader);
  assign dataIn_AXIS_port_fire_4 = (dataIn_AXIS_port_valid && dataIn_AXIS_port_ready);
  assign when_AXI4StreamInsertTopLevel_l159_3 = (mappedData_3_1 && issueCacheReady);
  assign when_AXI4StreamInsertTopLevel_l164_3 = (receiveComplete && issueCacheReady);
  assign combineData = {{{issueCache_0,issueCache_1},issueCache_2},issueCache_3};
  assign dataIn_AXIS_port_fire_5 = (dataIn_AXIS_port_valid && dataIn_AXIS_port_ready);
  assign when_AXI4StreamInsertTopLevel_l180 = (receiveComplete && issueCacheReady);
  assign when_AXI4StreamInsertTopLevel_l182 = (issueCacheLoaded && issueCacheReady);
  assign dataOut_AXIS_port_valid = issueCacheLoaded;
  assign dataOut_AXIS_port_payload_data = combineData;
  assign when_AXI4StreamInsertTopLevel_l190 = ((receiveComplete && dataOut_AXIS_port_ready) && (delayCnt == _zz_when_AXI4StreamInsertTopLevel_l190));
  always @(*) begin
    if(when_AXI4StreamInsertTopLevel_l190) begin
      dataOut_AXIS_port_payload_last = 1'b1;
    end else begin
      dataOut_AXIS_port_payload_last = 1'b0;
    end
  end

  always @(*) begin
    if(when_AXI4StreamInsertTopLevel_l190) begin
      dataOut_AXIS_port_payload_keep = lastKeep;
    end else begin
      dataOut_AXIS_port_payload_keep = 4'b1111;
    end
  end

  always @(posedge clk or posedge reset) begin
    if(reset) begin
      getHeader <= 1'b0;
      receiveComplete <= 1'b0;
      selectedMode <= 4'b0000;
      remainCnt <= 4'b0000;
      delayCnt <= 4'b0000;
      dataCache_0_0 <= 8'h0;
      dataCache_0_1 <= 1'b0;
      dataCache_1_0 <= 8'h0;
      dataCache_1_1 <= 1'b0;
      dataCache_2_0 <= 8'h0;
      dataCache_2_1 <= 1'b0;
      dataCache_3_0 <= 8'h0;
      dataCache_3_1 <= 1'b0;
      issueCache_0 <= 8'h0;
      issueCache_1 <= 8'h0;
      issueCache_2 <= 8'h0;
      issueCache_3 <= 8'h0;
      issueCacheLoaded <= 1'b0;
    end else begin
      if(headerIn_AXIS_port_fire) begin
        getHeader <= 1'b1;
      end else begin
        if(dataOut_AXIS_port_payload_last) begin
          getHeader <= 1'b0;
        end else begin
          getHeader <= getHeader;
        end
      end
      if(dataIn_AXIS_port_payload_last) begin
        receiveComplete <= 1'b1;
      end else begin
        if(dataOut_AXIS_port_payload_last) begin
          receiveComplete <= 1'b0;
        end else begin
          receiveComplete <= receiveComplete;
        end
      end
      if(headerIn_AXIS_port_fire_1) begin
        selectedMode <= headerIn_AXIS_port_payload_user;
      end else begin
        selectedMode <= selectedMode;
      end
      if(dataIn_AXIS_port_payload_last) begin
        remainCnt <= (selectedMode + _zz_remainCnt_8);
      end else begin
        remainCnt <= remainCnt;
      end
      if(when_AXI4StreamInsertTopLevel_l60) begin
        delayCnt <= 4'b0000;
      end else begin
        if(when_AXI4StreamInsertTopLevel_l62) begin
          delayCnt <= (delayCnt + 4'b0001);
        end else begin
          delayCnt <= delayCnt;
        end
      end
      if(dataIn_AXIS_port_fire) begin
        dataCache_0_0 <= dataIn_AXIS_port_payload_data[7 : 0];
        dataCache_0_1 <= dataIn_AXIS_port_payload_keep[0];
        dataCache_1_0 <= dataIn_AXIS_port_payload_data[15 : 8];
        dataCache_1_1 <= dataIn_AXIS_port_payload_keep[1];
        dataCache_2_0 <= dataIn_AXIS_port_payload_data[23 : 16];
        dataCache_2_1 <= dataIn_AXIS_port_payload_keep[2];
        dataCache_3_0 <= dataIn_AXIS_port_payload_data[31 : 24];
        dataCache_3_1 <= dataIn_AXIS_port_payload_keep[3];
      end else begin
        dataCache_0_0 <= dataCache_0_0;
        dataCache_0_1 <= dataCache_0_1;
        dataCache_1_0 <= dataCache_1_0;
        dataCache_1_1 <= dataCache_1_1;
        dataCache_2_0 <= dataCache_2_0;
        dataCache_2_1 <= dataCache_2_1;
        dataCache_3_0 <= dataCache_3_0;
        dataCache_3_1 <= dataCache_3_1;
      end
      if(when_AXI4StreamInsertTopLevel_l151) begin
        if(mappedHeader_0_1) begin
          issueCache_0 <= mappedHeader_0_0;
        end else begin
          issueCache_0 <= issueCache_0;
        end
      end else begin
        if(dataIn_AXIS_port_fire_1) begin
          if(when_AXI4StreamInsertTopLevel_l159) begin
            issueCache_0 <= mappedData_0_0;
          end else begin
            issueCache_0 <= issueCache_0;
          end
        end else begin
          if(when_AXI4StreamInsertTopLevel_l164) begin
            issueCache_0 <= mappedData_0_0;
          end else begin
            issueCache_0 <= issueCache_0;
          end
        end
      end
      if(when_AXI4StreamInsertTopLevel_l151_1) begin
        if(mappedHeader_1_1) begin
          issueCache_1 <= mappedHeader_1_0;
        end else begin
          issueCache_1 <= issueCache_1;
        end
      end else begin
        if(dataIn_AXIS_port_fire_2) begin
          if(when_AXI4StreamInsertTopLevel_l159_1) begin
            issueCache_1 <= mappedData_1_0;
          end else begin
            issueCache_1 <= issueCache_1;
          end
        end else begin
          if(when_AXI4StreamInsertTopLevel_l164_1) begin
            issueCache_1 <= mappedData_1_0;
          end else begin
            issueCache_1 <= issueCache_1;
          end
        end
      end
      if(when_AXI4StreamInsertTopLevel_l151_2) begin
        if(mappedHeader_2_1) begin
          issueCache_2 <= mappedHeader_2_0;
        end else begin
          issueCache_2 <= issueCache_2;
        end
      end else begin
        if(dataIn_AXIS_port_fire_3) begin
          if(when_AXI4StreamInsertTopLevel_l159_2) begin
            issueCache_2 <= mappedData_2_0;
          end else begin
            issueCache_2 <= issueCache_2;
          end
        end else begin
          if(when_AXI4StreamInsertTopLevel_l164_2) begin
            issueCache_2 <= mappedData_2_0;
          end else begin
            issueCache_2 <= issueCache_2;
          end
        end
      end
      if(when_AXI4StreamInsertTopLevel_l151_3) begin
        if(mappedHeader_3_1) begin
          issueCache_3 <= mappedHeader_3_0;
        end else begin
          issueCache_3 <= issueCache_3;
        end
      end else begin
        if(dataIn_AXIS_port_fire_4) begin
          if(when_AXI4StreamInsertTopLevel_l159_3) begin
            issueCache_3 <= mappedData_3_0;
          end else begin
            issueCache_3 <= issueCache_3;
          end
        end else begin
          if(when_AXI4StreamInsertTopLevel_l164_3) begin
            issueCache_3 <= mappedData_3_0;
          end else begin
            issueCache_3 <= issueCache_3;
          end
        end
      end
      if(dataIn_AXIS_port_fire_5) begin
        issueCacheLoaded <= 1'b1;
      end else begin
        if(dataOut_AXIS_port_payload_last) begin
          issueCacheLoaded <= 1'b0;
        end else begin
          if(when_AXI4StreamInsertTopLevel_l180) begin
            issueCacheLoaded <= 1'b1;
          end else begin
            if(when_AXI4StreamInsertTopLevel_l182) begin
              issueCacheLoaded <= 1'b0;
            end else begin
              issueCacheLoaded <= issueCacheLoaded;
            end
          end
        end
      end
    end
  end


endmodule
