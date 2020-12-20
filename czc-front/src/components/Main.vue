<template>
  <div id="app" style="background: #99a9bf">
    <el-row>
      <el-col :span="24">
        <div class="grid-content bg-purple-dark"></div>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="7">
        <div class="grid-content bg-purple-dark"></div>
      </el-col>
      <el-col :span="3">
        <div class="grid-content bg-purple-dark">
          <el-input v-model="input" placeholder="商品id"></el-input>
        </div>
      </el-col>
      <el-col :span="1">
        <div class="grid-content bg-purple-dark"></div>
      </el-col>
      <el-col :span="3">
        <div class="grid-content bg-purple-dark">
          <el-input v-model="input" placeholder="cookies"></el-input>
        </div>
      </el-col>
      <el-col :span="1">
        <div class="grid-content bg-purple-dark"></div>
      </el-col>
      <el-col :span="2">
        <div class="grid-content bg-purple-dark">
          <el-button type="primary">监控</el-button>
        </div>
      </el-col>
      <el-col :span="7">
        <div class="grid-content bg-purple-dark"></div>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="4">
        <div class="grid-content bg-purple-dark"></div>
      </el-col>
      <el-col :span="16">
        <div class="grid-content bg-purple-dark">
          <el-table :data="tableData" stripe style="width: 100%">
            <el-table-column prop="date" label="日期" width="180"></el-table-column>
            <el-table-column prop="name" label="姓名" width="180"></el-table-column>
            <el-table-column prop="address" label="地址"></el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="grid-content bg-purple-dark"></div>
      </el-col>
    </el-row>
    <el-row>
      <el-col :span="24">
        <div class="grid-content bg-purple-dark"></div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  data () {
    return {
      tableData: [
        {
          date: '2016-05-02',
          name: '王小虎',
          address: '上海市普陀区金沙江路 1518 弄'
        },
        {
          date: '2016-05-04',
          name: '王小虎',
          address: '上海市普陀区金沙江路 1517 弄'
        },
        {
          date: '2016-05-01',
          name: '王小虎',
          address: '上海市普陀区金沙江路 1519 弄'
        },
        {
          date: '2016-05-03',
          name: '王小虎',
          address: '上海市普陀区金沙江路 1516 弄'
        }
      ]
    }
  }
}
</script>

<style>
.el-row {
  margin-bottom: 20px;
}
.el-col {
  border-radius: 4px;
}
.bg-purple-dark {
  background: #99a9bf;
}
.bg-purple {
  background: #d3dce6;
}
.bg-purple-light {
  background: #e5e9f2;
}
.grid-content {
  border-radius: 4px;
  min-height: 42px;
  max-height: 840px;
}
.row-bg {
  padding: 10px 0;
  background-color: #f9fafc;
}
</style>
